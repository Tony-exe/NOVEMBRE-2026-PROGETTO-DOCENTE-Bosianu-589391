package it.uniroma3.siw.festival.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw.festival.exception.ConflittoProgrammazioneException;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.exception.RegolaBusinessException;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Sala;
import it.uniroma3.siw.festival.model.StatoProiezione;
import it.uniroma3.siw.festival.repository.FestivalRepository;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;
import it.uniroma3.siw.festival.repository.SalaRepository;

@Service
@Transactional(readOnly = true)
public class ProiezioneService {

    private static final Logger log = LoggerFactory.getLogger(ProiezioneService.class);

    private final ProiezioneRepository proiezioneRepository;
    private final FestivalRepository festivalRepository;
    private final FilmRepository filmRepository;
    private final SalaRepository salaRepository;

    public ProiezioneService(ProiezioneRepository proiezioneRepository, FestivalRepository festivalRepository,
            FilmRepository filmRepository, SalaRepository salaRepository) {
        this.proiezioneRepository = proiezioneRepository;
        this.festivalRepository = festivalRepository;
        this.filmRepository = filmRepository;
        this.salaRepository = salaRepository;
    }

    /** Programma del festival (film e sala già caricati), eventualmente filtrato per giorno. */
    public List<Proiezione> programma(Long festivalId, LocalDate data) {
        if (!festivalRepository.existsById(festivalId)) {
            throw new EntityNotFoundException("Festival", festivalId);
        }
        return data == null
                ? proiezioneRepository.findProgramma(festivalId)
                : proiezioneRepository.findProgrammaDelGiorno(festivalId, data);
    }

    public Proiezione trova(Long id) {
        return proiezioneRepository.findByIdWithDettagli(id)
                .orElseThrow(() -> new EntityNotFoundException("Proiezione", id));
    }

    /*
     * CASO D'USO MULTI-ENTITÀ PRINCIPALE.
     * Coinvolge quattro entità (Festival, Film, Sala, Proiezione) e quattro repository in UNA transazione:
     * qualsiasi eccezione (tutte unchecked) provoca il rollback completo, quindi non restano dati parziali.
     *
     * Isolamento SERIALIZABLE (linee guida della lezione 07): il caso d'uso LEGGE le proiezioni della sala
     * per DECIDERE se può scrivere. Con READ_COMMITTED (default di PostgreSQL) due transazioni concorrenti
     * nella stessa sala potrebbero entrambe verificare "nessuna sovrapposizione" (nessuna vede l'INSERT non
     * ancora confermato dell'altra) e poi inserire entrambe: inserimento fantasma / write skew.
     * Con SERIALIZABLE PostgreSQL si accorge del conflitto e annulla una delle due transazioni con un errore
     * di serializzazione (SQLState 40001 → ConcurrencyFailureException in Spring), che il gestore globale
     * degli errori traduce in 409 "riprova". Prezzo: qualche transazione annullata in più sotto carico.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Proiezione programmaProiezione(Long festivalId, Long filmId, Long salaId, LocalDate data, LocalTime ora) {
        // 1. recupero festival e film (404 se mancano)
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new EntityNotFoundException("Festival", festivalId));
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new EntityNotFoundException("Film", filmId));

        // 2. regole sul festival e sull'orario
        if (!festivalRepository.partecipaAlFestival(festivalId, filmId)) {
            throw new RegolaBusinessException("Il film \"" + film.getTitolo() + "\" non partecipa al festival");
        }
        verificaDataEOra(festival, data, ora);
        LocalTime oraFine = calcolaOraFine(ora, film);

        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new EntityNotFoundException("Sala", salaId));

        // 3. verifica delle sovrapposizioni nella sala
        verificaDisponibilita(sala, data, ora, oraFine, null);

        // 4-5. creazione e aggiornamento dei due lati delle associazioni bidirezionali
        Proiezione proiezione = new Proiezione();
        festival.addProiezione(proiezione);
        film.addProiezione(proiezione);
        sala.addProiezione(proiezione);
        proiezione.impostaOrario(data, ora);
        Proiezione salvata = proiezioneRepository.save(proiezione);
        log.info("Proiezione programmata: id={}, festival={}, film={}, sala={}, {} {}",
                salvata.getId(), festivalId, filmId, salaId, data, ora);
        return salvata;
    }

    /** Sposta una proiezione programmata (sala, data e ora). Stessi controlli della programmazione. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Proiezione modificaProiezione(Long id, Long salaId, LocalDate data, LocalTime ora) {
        Proiezione proiezione = trova(id);
        verificaModificabile(proiezione);
        verificaDataEOra(proiezione.getFestival(), data, ora);
        LocalTime oraFine = calcolaOraFine(ora, proiezione.getFilm());

        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() -> new EntityNotFoundException("Sala", salaId));
        // escludo la proiezione stessa, altrimenti si "sovrapporrebbe" con la sua vecchia versione
        verificaDisponibilita(sala, data, ora, oraFine, proiezione.getId());

        proiezione.setSala(sala);
        proiezione.impostaOrario(data, ora);
        log.info("Proiezione modificata: id={}, sala={}, {} {}", id, salaId, data, ora);
        return proiezione; // managed: l'UPDATE parte al commit
    }

    /** Cancellazione di una proiezione = passaggio allo stato CANCELLED (si conserva lo storico). */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void annulla(Long id) {
        Proiezione proiezione = trova(id);
        verificaModificabile(proiezione);
        proiezione.setStato(StatoProiezione.CANCELLED);
        log.info("Proiezione annullata: id={}", id);
    }

    /** Segna come svolta una proiezione già iniziata. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void segnaCompletata(Long id) {
        Proiezione proiezione = trova(id);
        verificaModificabile(proiezione);
        if (LocalDateTime.of(proiezione.getData(), proiezione.getOra()).isAfter(LocalDateTime.now())) {
            throw new RegolaBusinessException("Una proiezione futura non può essere segnata come completata");
        }
        proiezione.setStato(StatoProiezione.COMPLETED);
        log.info("Proiezione completata: id={}", id);
    }

    private void verificaModificabile(Proiezione proiezione) {
        if (!proiezione.isModificabile()) {
            throw new RegolaBusinessException("Una proiezione " + proiezione.getStato() + " non è modificabile");
        }
    }

    private void verificaDataEOra(Festival festival, LocalDate data, LocalTime ora) {
        if (!festival.comprendeData(data)) {
            throw new RegolaBusinessException("La data deve essere compresa tra " + festival.getDataInizio()
                    + " e " + festival.getDataFine());
        }
        if (LocalDateTime.of(data, ora).isBefore(LocalDateTime.now())) {
            throw new RegolaBusinessException("Non si possono programmare proiezioni nel passato");
        }
    }

    // La proiezione deve finire entro la mezzanotte: così inizio e fine sono nello stesso giorno
    // e il confronto tra orari nella query di sovrapposizione resta corretto
    private LocalTime calcolaOraFine(LocalTime ora, Film film) {
        LocalTime oraFine = ora.plusMinutes(film.getDurata());
        if (!oraFine.isAfter(ora)) {
            throw new RegolaBusinessException("La proiezione terminerebbe dopo la mezzanotte");
        }
        return oraFine;
    }

    private void verificaDisponibilita(Sala sala, LocalDate data, LocalTime ora, LocalTime oraFine, Long escludiId) {
        if (proiezioneRepository.esisteSovrapposizione(sala.getId(), data, ora, oraFine, escludiId)) {
            log.warn("Sovrapposizione rifiutata: sala={}, {} {}-{}", sala.getId(), data, ora, oraFine);
            throw new ConflittoProgrammazioneException("La sala \"" + sala.getNome() + "\" è già occupata il "
                    + data + " tra le " + ora + " e le " + oraFine);
        }
    }
}
