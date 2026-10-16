package it.uniroma3.siw.festival.service;

import java.util.List;

import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Recensione;

/**
 * Tutto ciò che serve al dettaglio di un film, già caricato dal service con query separate
 * (film + regista + cast, festival, proiezioni con sala e festival, recensioni con autore).
 * Non è un DTO JSON: il controller REST lo converte nei propri DTO.
 *
 * @param mediaVoti null se il film non ha recensioni
 */
public record DettaglioFilm(Film film, List<Festival> festivals, List<Proiezione> proiezioni,
        List<Recensione> recensioni, Double mediaVoti) {
}
