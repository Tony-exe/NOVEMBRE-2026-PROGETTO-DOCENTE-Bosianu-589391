package it.uniroma3.siw.festival.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw.festival.dto.form.RecensioneForm;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.exception.OperazioneNonAutorizzataException;
import it.uniroma3.siw.festival.exception.RecensioneDuplicataException;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Recensione;
import it.uniroma3.siw.festival.model.Utente;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.RecensioneRepository;
import it.uniroma3.siw.festival.repository.UtenteRepository;

/**
 * Recensioni. Lo username dell'autore arriva dal controller, che lo prende dall'utente autenticato
 * (mai da un parametro della richiesta): il controllo di proprietà è qui, non nella UI.
 */
@Service
@Transactional(readOnly = true)
public class RecensioneService {

    /*
     * Sicurezza a livello di metodo: serve un utente registrato E lo username passato al metodo
     * (#username) deve essere quello dell'utente autenticato. Così nessun chiamante può agire
     * "a nome di" un altro. La proprietà della singola recensione si verifica invece nel codice
     * (trovaPropria), perché richiede di leggere l'autore dal database.
     */
    private static final String UTENTE_AUTENTICATO =
            "hasAnyRole('USER', 'ADMIN') and #username == authentication.name";

    private final RecensioneRepository recensioneRepository;
    private final FilmRepository filmRepository;
    private final UtenteRepository utenteRepository;

    public RecensioneService(RecensioneRepository recensioneRepository, FilmRepository filmRepository,
            UtenteRepository utenteRepository) {
        this.recensioneRepository = recensioneRepository;
        this.filmRepository = filmRepository;
        this.utenteRepository = utenteRepository;
    }

    public List<Recensione> recensioniDelFilm(Long filmId) {
        verificaFilmEsistente(filmId);
        return recensioneRepository.findByFilmIdWithUtente(filmId);
    }

    /** Media dei voti (null se non ci sono recensioni). */
    public Double mediaVoti(Long filmId) {
        verificaFilmEsistente(filmId);
        return recensioneRepository.mediaVoti(filmId);
    }

    @PreAuthorize(UTENTE_AUTENTICATO)
    @Transactional
    public Recensione crea(Long filmId, String username, RecensioneForm form) {
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new EntityNotFoundException("Film", filmId));
        Utente autore = trovaUtente(username);

        // Controllo preventivo: copre il caso normale con un messaggio chiaro
        if (recensioneRepository.existsByFilmIdAndUtenteId(filmId, autore.getId())) {
            throw new RecensioneDuplicataException("Hai già recensito \"" + film.getTitolo() + "\"");
        }
        Recensione recensione = new Recensione(form.getTesto(), form.getVoto(), LocalDate.now(), film, autore);
        try {
            // saveAndFlush esegue subito l'INSERT: se due richieste concorrenti superano entrambe il
            // controllo sopra, il vincolo unique del DB fa fallire la seconda, e la traduco nello stesso errore
            return recensioneRepository.saveAndFlush(recensione);
        } catch (DataIntegrityViolationException e) {
            throw new RecensioneDuplicataException("Hai già recensito \"" + film.getTitolo() + "\"");
        }
    }

    @PreAuthorize(UTENTE_AUTENTICATO)
    @Transactional
    public Recensione modifica(Long recensioneId, String username, RecensioneForm form) {
        Recensione recensione = trovaPropria(recensioneId, username);
        recensione.setTesto(form.getTesto());
        recensione.setVoto(form.getVoto());
        recensione.setData(LocalDate.now());
        return recensione;
    }

    @PreAuthorize(UTENTE_AUTENTICATO)
    @Transactional
    public void elimina(Long recensioneId, String username) {
        Recensione recensione = trovaPropria(recensioneId, username);
        recensioneRepository.delete(recensione);
    }

    /*
     * Controllo di proprietà: la recensione deve esistere (404) e l'autore deve essere l'utente
     * autenticato (403). Anche se la UI nasconde i pulsanti, una richiesta costruita a mano
     * (es. PUT /api/reviews/5 di un altro utente) viene bloccata qui.
     */
    private Recensione trovaPropria(Long recensioneId, String username) {
        Recensione recensione = recensioneRepository.findByIdWithUtenteAndFilm(recensioneId)
                .orElseThrow(() -> new EntityNotFoundException("Recensione", recensioneId));
        if (!recensione.getUtente().getUsername().equals(username)) {
            throw new OperazioneNonAutorizzataException("Puoi modificare o eliminare solo le tue recensioni");
        }
        return recensione;
    }

    private Utente trovaUtente(String username) {
        return utenteRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Utente " + username + " non trovato"));
    }

    private void verificaFilmEsistente(Long filmId) {
        if (!filmRepository.existsById(filmId)) {
            throw new EntityNotFoundException("Film", filmId);
        }
    }
}
