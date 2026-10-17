package it.uniroma3.siw.festival.dto.api;

import it.uniroma3.siw.festival.model.Film;

/** Film negli elenchi (film di un festival, ricerca). Richiede il regista già caricato. */
public record FilmDto(Long id, String titolo, Integer anno, Integer durata, String genere, String paeseProduzione,
        RegistaDto regista) {

    public static FilmDto da(Film f) {
        return new FilmDto(f.getId(), f.getTitolo(), f.getAnno(), f.getDurata(), f.getGenere(),
                f.getPaeseProduzione(), RegistaDto.da(f.getRegista()));
    }
}
