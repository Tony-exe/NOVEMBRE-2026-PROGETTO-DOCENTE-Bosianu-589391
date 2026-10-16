package it.uniroma3.siw.festival.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Gestione centralizzata delle eccezioni di dominio per i controller Thymeleaf: invece della pagina
 * di errore generica si mostra un template con il messaggio e il codice HTTP corretto.
 *
 * Le regole di business violate in un form (es. sovrapposizione) sono già gestite nel controller,
 * che rimostra il form con l'errore; qui arrivano i casi rimasti (entità inesistente, proprietà...).
 *
 * annotations = Controller.class vale anche per i @RestController (che sono @Controller): per questo
 * ha la precedenza più bassa e il @RestControllerAdvice delle API, più specifico, viene consultato prima.
 * Non gestisce AccessDeniedException: quella deve arrivare a Spring Security (login o pagina 403).
 */
@ControllerAdvice(annotations = Controller.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class MvcExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ModelAndView nonTrovato(EntityNotFoundException e) {
        return pagina("error/404", HttpStatus.NOT_FOUND, e.getMessage());
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

    @ExceptionHandler(RegolaBusinessException.class)
    public ModelAndView regolaViolata(RegolaBusinessException e) {
        return pagina("error/error", HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ModelAndView pagina(String vista, HttpStatus status, String messaggio) {
        ModelAndView mav = new ModelAndView(vista);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("messaggio", messaggio);
        return mav;
    }
}
