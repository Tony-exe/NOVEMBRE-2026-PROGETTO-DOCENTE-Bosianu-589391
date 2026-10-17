package it.uniroma3.siw.festival.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.uniroma3.siw.festival.dto.api.UtenteCorrenteDto;

/**
 * Dice a React chi è collegato. React condivide la sessione con le pagine Thymeleaf
 * (stesso dominio, stesso cookie JSESSIONID), quindi vede lo stesso utente.
 */
@RestController
@Tag(name = "Utente")
public class MeApiController {

    @Operation(summary = "Utente della sessione corrente ({autenticato: false} se anonimo)")
    @GetMapping("/api/me")
    public ResponseEntity<UtenteCorrenteDto> me(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.ok(UtenteCorrenteDto.anonimo());
        }
        // l'autorità è "ROLE_USER" o "ROLE_ADMIN": restituisco il ruolo senza prefisso
        String ruolo = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
        return ResponseEntity.ok(new UtenteCorrenteDto(true, authentication.getName(), ruolo));
    }
}
