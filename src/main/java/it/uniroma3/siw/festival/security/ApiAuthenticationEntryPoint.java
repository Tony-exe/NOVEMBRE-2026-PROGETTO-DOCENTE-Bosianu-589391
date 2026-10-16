package it.uniroma3.siw.festival.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cosa fare quando un utente NON autenticato chiede una risorsa protetta.
 * - pagine: redirect alla pagina di login (comportamento standard del form login);
 * - API (/api/**): 401 con un JSON ProblemDetail, perché React non deve ricevere l'HTML del login.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint loginPage = new LoginUrlAuthenticationEntryPoint("/login");
    private final ObjectMapper objectMapper;

    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        if (!isApi(request)) {
            loginPage.commence(request, response, authException);
            return;
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Devi effettuare il login per eseguire questa operazione");
        problema.setTitle("Non autenticato");
        scriviProblema(objectMapper, response, problema);
    }

    static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    static void scriviProblema(ObjectMapper objectMapper, HttpServletResponse response, ProblemDetail problema)
            throws IOException {
        response.setStatus(problema.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
