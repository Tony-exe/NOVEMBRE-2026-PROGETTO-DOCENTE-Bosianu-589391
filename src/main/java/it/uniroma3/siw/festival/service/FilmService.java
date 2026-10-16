package it.uniroma3.siw.festival.service;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw.festival.dto.form.FilmForm;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.exception.RegolaBusinessException;
import it.uniroma3.siw.festival.model.Attore;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Interpretazione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.StatoProiezione;
import it.uniroma3.siw.festival.repository.AttoreRepository;
import it.uniroma3.siw.festival.repository.FestivalRepository;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;
import it.uniroma3.siw.festival.repository.RecensioneRepository;
import it.uniroma3.siw.festival.repository.RegistaRepository;

@Service
@Transactional(readOnly = true)
public class FilmService {

    private static final int DIMENSIONE_MASSIMA_PAGINA = 50;

    private final FilmRepository filmRepository;
    private final RegistaRepository registaRepository;
    private final AttoreRepository attoreRepository;
    private final FestivalRepository festivalRepository;
    private final ProiezioneRepository proiezioneRepository;
    private final RecensioneRepository recensioneRepository;

    public FilmService(FilmRepository filmRepository, RegistaRepository registaRepository,
            AttoreRepository attoreRepository, FestivalRepository festivalRepository,
            ProiezioneRepository proiezioneRepository, RecensioneRepository recensioneRepository) {
        this.filmRepository = filmRepository;
        this.registaRepository = registaRepository;
        this.attoreRepository = attoreRepository;
        this.festivalRepository = festivalRepository;
        this.proiezioneRepository = proiezioneRepository;
        this.recensioneRepository = recensioneRepository;
    }

    /** Elenco completo con regista (per l'area amministrativa). */
    public List<Film> elenco() {
        return filmRepository.findAllWithRegista();
    }

    /** Ricerca per titolo, genere e regista (filtri opzionali), paginata e ordinata per titolo. */
    public Page<Film> cerca(String titolo, String genere, String regista, int pagina, int dimensione) {
        int size = Math.min(Math.max(dimensione, 1), DIMENSIONE_MASSIMA_PAGINA);
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), size, Sort.by("titolo"));
        return filmRepository.cerca(vuotoSeNull(titolo), vuotoSeNull(genere), vuotoSeNull(regista), pageable);
    }

    /** Film partecipanti a un festival, con il regista già caricato (JOIN FETCH: 1 query). */
    public List<Film> filmDelFestival(Long festivalId) {
        if (!festivalRepository.existsById(festivalId)) {
            throw new EntityNotFoundException("Festival", festivalId);
        }
        return filmRepository.findByFestivalIdWithRegista(festivalId);
    }

    /** Film che l'amministratore può ancora aggiungere al festival. */
    public List<Film> filmAssociabili(Long festivalId) {
        if (!festivalRepository.existsById(festivalId)) {
            throw new EntityNotFoundException("Festival", festivalId);
        }
        return filmRepository.findNonPartecipantiAlFestival(festivalId);
    }

    /** Film con regista e cast (per la gestione del cast e il form di modifica). */
    public Film trovaConCast(Long id) {
        return filmRepository.findByIdWithRegistaAndCast(id)
                .orElseThrow(() -> new EntityNotFoundException("Film", id));
    }

    /*
     * Dettaglio completo. Quattro query, una per "ramo": evito di mettere in JOIN FETCH più collezioni
     * nella stessa query (MultipleBagFetchException o prodotto cartesiano cast × proiezioni × recensioni).
     * Tutto avviene in un'unica transazione di sola lettura, quindi i dati sono coerenti tra loro.
     */
    public DettaglioFilm dettaglio(Long id) {
        Film film = trovaConCast(id);
        return new DettaglioFilm(
                film,
                festivalRepository.findByFilmId(id),
                proiezioneRepository.findByFilmIdWithSalaAndFestival(id),
                recensioneRepository.findByFilmIdWithUtente(id),
                recensioneRepository.mediaVoti(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Film crea(FilmForm form) {
        Film film = new Film();
        copiaDati(form, film);
        return filmRepository.save(film);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Film modifica(Long id, FilmForm form) {
        Film film = trovaConCast(id);
        // Le proiezioni programmate hanno oraFine calcolata con la vecchia durata: cambiarla
        // potrebbe creare sovrapposizioni non rilevate, quindi lo impedisco
        if (!Objects.equals(film.getDurata(), form.getDurata())
                && proiezioneRepository.existsByFilmIdAndStato(id, StatoProiezione.SCHEDULED)) {
            throw new RegolaBusinessException(
                    "Non puoi cambiare la durata di un film con proiezioni programmate");
        }
        copiaDati(form, film);
        return film;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void aggiungiInterpretazione(Long filmId, Long attoreId, String personaggio) {
        Film film = trovaConCast(filmId);
        Attore attore = attoreRepository.findById(attoreId)
                .orElseThrow(() -> new EntityNotFoundException("Attore", attoreId));
        String nome = personaggio == null ? "" : personaggio.trim();
        if (nome.isEmpty()) {
            throw new RegolaBusinessException("Indica il personaggio interpretato");
        }
        boolean giaPresente = film.getInterpretazioni().stream()
                .anyMatch(i -> i.getAttore().equals(attore) && i.getPersonaggio().equalsIgnoreCase(nome));
        if (giaPresente) {
            throw new RegolaBusinessException("Questo attore interpreta già questo personaggio nel film");
        }
        // Grazie a cascade = ALL sul film, la nuova Interpretazione viene salvata al commit
        film.addInterpretazione(attore, nome);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void rimuoviInterpretazione(Long filmId, Long interpretazioneId) {
        Film film = trovaConCast(filmId);
        Interpretazione interpretazione = film.getInterpretazioni().stream()
                .filter(i -> i.getId().equals(interpretazioneId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Interpretazione", interpretazioneId));
        // orphanRemoval = true: tolta dalla lista, l'interpretazione viene cancellata dal DB
        film.removeInterpretazione(interpretazione);
    }

    private void copiaDati(FilmForm form, Film film) {
        Regista regista = registaRepository.findById(form.getRegistaId())
                .orElseThrow(() -> new EntityNotFoundException("Regista", form.getRegistaId()));
        film.setTitolo(form.getTitolo());
        film.setAnno(form.getAnno());
        film.setDurata(form.getDurata());
        film.setGenere(form.getGenere());
        film.setPaeseProduzione(form.getPaeseProduzione());
        film.setRegista(regista);
    }

    private static String vuotoSeNull(String filtro) {
        return filtro == null ? "" : filtro.trim();
    }
}
