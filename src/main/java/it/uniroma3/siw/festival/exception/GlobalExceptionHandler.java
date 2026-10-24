package it.uniroma3.siw.festival.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Gestore globale delle eccezioni per le pagine Thymeleaf (lezione 08): invece della White Label Error
 * Page o di uno stack trace, l'utente vede un template con un messaggio comprensibile e il codice HTTP giusto.
 *
 * - risorsa applicativa inesistente (EntityNotFoundException, lanciata dal service) → 404
 * - URL che nessun controller gestisce (NoResourceFoundException) → 404 "di routing"
 * - errori di dominio → 409 / 400 / 403
 * - qualunque altro errore inatteso → 500, con l'errore scritto nel log per chi mantiene l'applicazione
 *
 * Le regole di business violate in un form (es. sovrapposizione) sono già gestite nel controller,
 * che rimostra il form con l'errore (bindingResult.reject); qui arrivano i casi rimasti.
 *
 * Senza selettori vale anche per i @RestController: per questo ha la precedenza più bassa e
 * l'ApiExceptionHandler (ProblemDetail JSON), più specifico, viene consultato prima.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ModelAndView nonTrovato(EntityNotFoundException e) {
        return pagina("error/404", HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView urlInesistente(NoResourceFoundException e) {
        return pagina("error/404", HttpStatus.NOT_FOUND, "Pagina non trovata");
    }

    @ExceptionHandler(OperazioneNonAutorizzataException.class)
    public ModelAndView nonAutorizzato(OperazioneNonAutorizzataException e) {
        return pagina("error/403", HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler({ ConflittoProgrammazioneException.class, RecensioneDuplicataException.class,
            UsernameGiaUsatoException.class })
    public ModelAndView conflitto(RuntimeException e) {
        return pagina("error/error", HttpStatus.CONFLICT, e.getMessage());
    }

    /*
     * Una transazione SERIALIZABLE annullata da PostgreSQL perché in conflitto con un'altra concorrente
     * (SQLState 40001): non è un bug, l'utente può semplicemente riprovare.
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ModelAndView concorrenza(ConcurrencyFailureException e) {
        log.warn("Transazione annullata per accesso concorrente: {}", e.getMessage());
        return pagina("error/error", HttpStatus.CONFLICT,
                "Un'altra operazione ha modificato gli stessi dati nello stesso momento. Riprova.");
    }

    @ExceptionHandler(RegolaBusinessException.class)
    public ModelAndView regolaViolata(RegolaBusinessException e) {
        return pagina("error/error", HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /*
     * Errori inattesi → 500. Tre eccezioni vengono rilanciate perché hanno già chi le gestisce:
     * AccessDeniedException e AuthenticationException devono arrivare a Spring Security (login o pagina 403),
     * le eccezioni standard di Spring MVC (ErrorResponse: parametro del tipo sbagliato, metodo non
     * supportato...) hanno già il loro codice 4xx.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView inatteso(Exception e) throws Exception {
        if (e instanceof AccessDeniedException || e instanceof AuthenticationException || e instanceof ErrorResponse) {
            throw e;
        }
        log.error("Errore inatteso", e);
        return pagina("error/500", HttpStatus.INTERNAL_SERVER_ERROR,
                "Si è verificato un errore interno. Riprovare più tardi.");
    }

    private ModelAndView pagina(String vista, HttpStatus status, String messaggio) {
        ModelAndView mav = new ModelAndView(vista);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("messaggio", messaggio);
        return mav;
    }
}
