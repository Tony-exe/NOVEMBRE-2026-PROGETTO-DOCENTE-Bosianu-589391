package it.uniroma3.siw.festival.dto.api;

import java.util.List;

import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.service.DettaglioFilm;

/**
 * Dettaglio di un film: informazioni, regista, cast, festival e proiezioni, più media e numero
 * delle recensioni. L'elenco delle recensioni ha un endpoint dedicato (GET /api/movies/{id}/reviews),
 * che React ricarica dopo ogni inserimento/modifica.
 */
public record FilmDettaglioDto(Long id, String titolo, Integer anno, Integer durata, String genere,
        String paeseProduzione, RegistaDto regista, List<Interprete> cast, List<FestivalDto> festivals,
        List<ProiezioneDto> proiezioni, Double mediaVoti, int numeroRecensioni) {

    public record Interprete(Long attoreId, String nome, String cognome, String personaggio) {
    }

    public static FilmDettaglioDto da(DettaglioFilm dettaglio) {
        Film f = dettaglio.film();
        List<Interprete> cast = f.getInterpretazioni().stream()
                .map(i -> new Interprete(i.getAttore().getId(), i.getAttore().getNome(), i.getAttore().getCognome(),
                        i.getPersonaggio()))
                .toList();
        return new FilmDettaglioDto(f.getId(), f.getTitolo(), f.getAnno(), f.getDurata(), f.getGenere(),
                f.getPaeseProduzione(), RegistaDto.da(f.getRegista()), cast,
                dettaglio.festivals().stream().map(FestivalDto::da).toList(),
                dettaglio.proiezioni().stream().map(ProiezioneDto::perFilm).toList(),
                dettaglio.mediaVoti(), dettaglio.recensioni().size());
    }
}
