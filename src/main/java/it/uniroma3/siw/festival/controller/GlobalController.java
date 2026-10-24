package it.uniroma3.siw.festival.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Mette a disposizione di TUTTI i template l'utente autenticato, come attributo "userDetails" del
 * modello (lezione 10). Spring Security conserva l'utente nel SecurityContext; per un visitatore non
 * autenticato l'Authentication è un AnonymousAuthenticationToken e l'attributo vale null.
 * La navbar lo usa per mostrare lo username.
 */
@ControllerAdvice
public class GlobalController {

    @ModelAttribute("userDetails")
    public UserDetails getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof UserDetails utente)) {
            return null;
        }
        return utente;
    }
}
