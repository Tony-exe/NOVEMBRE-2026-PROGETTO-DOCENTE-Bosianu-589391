package it.uniroma3.siw.festival.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cosa fare quando un utente autenticato non ha il permesso (ruolo sbagliato o token CSRF mancante).
 * - pagine: errore 403, mostrato da Spring Boot con la pagina templates/error/403.html;
 * - API: 403 con un JSON ProblemDetail.
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        if (!ApiAuthenticationEntryPoint.isApi(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String dettaglio = accessDeniedException instanceof CsrfException
                ? "Token CSRF mancante o non valido"
                : "Non hai i permessi per eseguire questa operazione";
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, dettaglio);
        problema.setTitle("Accesso negato");
        ApiAuthenticationEntryPoint.scriviProblema(objectMapper, response, problema);
    }
}
