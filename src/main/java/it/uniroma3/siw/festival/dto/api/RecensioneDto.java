package it.uniroma3.siw.festival.dto.api;

import java.time.LocalDate;

import it.uniroma3.siw.festival.model.Recensione;

/** Recensione in JSON: dell'autore si espone solo lo username (mai password o ruolo). */
public record RecensioneDto(Long id, Long filmId, String testo, Integer voto, LocalDate data, String autore) {

    /** Richiede l'utente già caricato (JOIN FETCH r.utente). */
    public static RecensioneDto da(Recensione r) {
        return new RecensioneDto(r.getId(), r.getFilm().getId(), r.getTesto(), r.getVoto(), r.getData(),
                r.getUtente().getUsername());
    }
}
