package it.uniroma3.siw.festival.security;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security 6 carica il token CSRF in modo "differito": il cookie XSRF-TOKEN viene scritto
 * solo se qualcuno legge il token (es. un form Thymeleaf con th:action). Le pagine React non lo
 * leggono lato server, quindi il cookie non arriverebbe mai al browser.
 * Questo filtro legge il token a ogni richiesta, forzandone il salvataggio nel cookie.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // il caricamento del token fa scrivere il cookie
        }
        filterChain.doFilter(request, response);
    }
}
