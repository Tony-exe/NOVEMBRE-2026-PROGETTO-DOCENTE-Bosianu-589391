package it.uniroma3.siw.festival.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw.festival.dto.form.FestivalForm;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.exception.RegolaBusinessException;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.StatoProiezione;
import it.uniroma3.siw.festival.repository.FestivalRepository;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;

/*
 * Default di classe: tutte le operazioni sono di sola lettura. readOnly = true dice a Hibernate
 * di non fare dirty checking né flush (meno lavoro) e documenta che il metodo non scrive.
 * I metodi che scrivono sovrascrivono l'annotazione con @Transactional "pieno".
 * @PreAuthorize sui metodi di scrittura: secondo livello di sicurezza, oltre alla regola /admin/**
 * della SecurityFilterChain (se manca il ruolo, il proxy lancia AccessDeniedException prima del metodo).
 */
@Service
@Transactional(readOnly = true)
public class FestivalService {

    private final FestivalRepository festivalRepository;
    private final FilmRepository filmRepository;
    private final ProiezioneRepository proiezioneRepository;

    public FestivalService(FestivalRepository festivalRepository, FilmRepository filmRepository,
            ProiezioneRepository proiezioneRepository) {
        this.festivalRepository = festivalRepository;
        this.filmRepository = filmRepository;
        this.proiezioneRepository = proiezioneRepository;
    }

    public List<Festival> elenco() {
        return festivalRepository.findAllByOrderByDataInizioDesc();
    }

    public Festival trova(Long id) {
        return festivalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Festival", id));
    }

    /** Festival con la collezione dei film già caricata (utilizzabile anche fuori dalla transazione). */
    public Festival trovaConFilm(Long id) {
        return festivalRepository.findByIdWithFilms(id)
                .orElseThrow(() -> new EntityNotFoundException("Festival", id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Festival crea(FestivalForm form) {
        verificaPeriodo(form);
        Festival festival = new Festival();
        copiaDati(form, festival);
        return festivalRepository.save(festival);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Festival modifica(Long id, FestivalForm form) {
        verificaPeriodo(form);
        Festival festival = trova(id);
        if (proiezioneRepository.esistonoProiezioniFuoriPeriodo(id, form.getDataInizio(), form.getDataFine())) {
            throw new RegolaBusinessException(
                    "Ci sono proiezioni programmate fuori dal nuovo periodo: annullale o spostale prima");
        }
        // Nessuna save: l'entità è "managed", al commit Hibernate rileva le modifiche (dirty checking)
        copiaDati(form, festival);
        return festival;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void aggiungiFilm(Long festivalId, Long filmId) {
        Festival festival = trovaConFilm(festivalId);
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new EntityNotFoundException("Film", filmId));
        if (festival.getFilms().contains(film)) {
            throw new RegolaBusinessException("Il film partecipa già a questo festival");
        }
        // Festival è il lato proprietario: aggiungere qui produce l'INSERT in festival_film
        festival.addFilm(film);
    }

    /**
     * Rimuove un film da un festival. Regola di business: le proiezioni ancora programmate di quel film
     * in quel festival vengono annullate (CANCELLED, non eliminate, per conservarne lo storico);
     * quelle già svolte (COMPLETED) restano invariate.
     * Tutto avviene nella stessa transazione: se qualcosa fallisce, né l'associazione né le
     * proiezioni vengono modificate.
     *
     * @return il numero di proiezioni annullate
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public int rimuoviFilm(Long festivalId, Long filmId) {
        Festival festival = trovaConFilm(festivalId);
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new EntityNotFoundException("Film", filmId));
        if (!festival.getFilms().contains(film)) {
            throw new RegolaBusinessException("Il film non partecipa a questo festival");
        }

        festival.removeFilm(film); // DELETE della riga in festival_film al commit

        int annullate = 0;
        for (Proiezione proiezione : proiezioneRepository.findByFestivalIdAndFilmId(festivalId, filmId)) {
            if (proiezione.getStato() == StatoProiezione.SCHEDULED) {
                proiezione.setStato(StatoProiezione.CANCELLED); // UPDATE al commit (dirty checking)
                annullate++;
            }
        }
        return annullate;
    }

    // Ricontrollo nel service anche se il form ha @PeriodoValido: il service può essere chiamato
    // da altri punti (API, test) e la regola deve valere sempre
    private void verificaPeriodo(FestivalForm form) {
        if (form.getDataFine().isBefore(form.getDataInizio())) {
            throw new RegolaBusinessException("La data di fine non può precedere la data di inizio");
        }
    }

    private void copiaDati(FestivalForm form, Festival festival) {
        festival.setNome(form.getNome());
        festival.setAnno(form.getAnno());
        festival.setCitta(form.getCitta());
        festival.setDataInizio(form.getDataInizio());
        festival.setDataFine(form.getDataFine());
        festival.setDescrizione(form.getDescrizione());
    }
}
