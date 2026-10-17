package it.uniroma3.siw.festival.exception;

import java.util.List;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.uniroma3.siw.festival.controller.api.FestivalApiController;

/**
 * Gestione centralizzata degli errori delle API REST: ogni errore diventa un ProblemDetail (RFC 7807),
 * cioè un JSON standard { type, title, status, detail, instance } con Content-Type application/problem+json.
 *
 * Estende ResponseEntityExceptionHandler, che converte già in ProblemDetail le eccezioni standard di
 * Spring MVC (JSON malformato → 400, parametro del tipo sbagliato → 400, metodo non supportato → 405...).
 * Qui si aggiungono le eccezioni di dominio e si arricchisce il 400 di validazione con l'elenco dei campi.
 *
 * Vale solo per i controller del package controller.api e ha la precedenza più alta, quindi viene
 * consultato prima di MvcExceptionHandler (che restituisce pagine HTML).
 * 401 e 403 dovuti a Spring Security non passano di qui: li producono i filtri, prima dei controller
 * (ApiAuthenticationEntryPoint, ApiAccessDeniedHandler), sempre in formato ProblemDetail.
 */
@RestControllerAdvice(basePackageClasses = FestivalApiController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail nonTrovato(EntityNotFoundException e) {
        return problema(HttpStatus.NOT_FOUND, "Risorsa non trovata", e.getMessage());
    }

    @ExceptionHandler(OperazioneNonAutorizzataException.class)
    public ProblemDetail nonAutorizzato(OperazioneNonAutorizzataException e) {
        return problema(HttpStatus.FORBIDDEN, "Operazione non consentita", e.getMessage());
    }

    @ExceptionHandler({ RecensioneDuplicataException.class, ConflittoProgrammazioneException.class })
    public ProblemDetail conflitto(RuntimeException e) {
        return problema(HttpStatus.CONFLICT, "Conflitto", e.getMessage());
    }

    // Ultima rete di sicurezza: un vincolo del DB violato (es. unique) che il service non ha intercettato
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail vincoloViolato(DataIntegrityViolationException e) {
        return problema(HttpStatus.CONFLICT, "Conflitto", "L'operazione viola un vincolo di integrità dei dati");
    }

    @ExceptionHandler(RegolaBusinessException.class)
    public ProblemDetail regolaViolata(RegolaBusinessException e) {
        return problema(HttpStatus.BAD_REQUEST, "Regola di business violata", e.getMessage());
    }

    /*
     * Corpo JSON non valido rispetto ai vincoli di Bean Validation (@Valid @RequestBody):
     * 400 con la proprietà "errori" = elenco di { campo, messaggio }, che React mostra accanto ai campi.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errori = ex.getBindingResult().getFieldErrors().stream()
                .map(errore -> Map.of("campo", errore.getField(),
                        "messaggio", String.valueOf(errore.getDefaultMessage())))
                .toList();
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dati non validi",
                "Alcuni campi non sono validi");
        problema.setProperty("errori", errori);
        return ResponseEntity.badRequest().headers(headers).body(problema);
    }

    private static ProblemDetail problema(HttpStatus status, String titolo, String dettaglio) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, dettaglio);
        problema.setTitle(titolo);
        return problema;
    }
}
