package it.uniroma3.siw.festival.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Chi è l'utente della sessione corrente: React lo usa per decidere cosa mostrare. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UtenteCorrenteDto(boolean autenticato, String username, String ruolo) {

    public static UtenteCorrenteDto anonimo() {
        return new UtenteCorrenteDto(false, null, null);
    }
}
