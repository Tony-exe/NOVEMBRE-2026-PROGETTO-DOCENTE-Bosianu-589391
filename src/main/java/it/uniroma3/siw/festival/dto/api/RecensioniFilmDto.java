package it.uniroma3.siw.festival.dto.api;

import java.util.List;

/** Recensioni di un film con la media dei voti (null se non ci sono recensioni). */
public record RecensioniFilmDto(Long filmId, Double mediaVoti, int numero, List<RecensioneDto> recensioni) {
}
