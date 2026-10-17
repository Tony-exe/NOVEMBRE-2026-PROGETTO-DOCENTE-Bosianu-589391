package it.uniroma3.siw.festival.dto.api;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.StatoProiezione;

/**
 * Proiezione in JSON. I riferimenti (film, sala, festival) sono piccoli oggetti id + nome.
 * Due costruttori perché i due casi d'uso caricano associazioni diverse: usare quello sbagliato
 * toccherebbe un proxy LAZY fuori dalla transazione. I campi null non vengono serializzati.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProiezioneDto(Long id, LocalDate data, LocalTime ora, LocalTime oraFine, StatoProiezione stato,
        Riferimento film, Riferimento sala, Riferimento festival) {

    public record Riferimento(Long id, String nome) {
    }

    /** Programma di un festival: film e sala caricati (ProiezioneRepository.findProgramma). */
    public static ProiezioneDto perProgramma(Proiezione p) {
        return new ProiezioneDto(p.getId(), p.getData(), p.getOra(), p.getOraFine(), p.getStato(),
                new Riferimento(p.getFilm().getId(), p.getFilm().getTitolo()),
                new Riferimento(p.getSala().getId(), p.getSala().getNome()),
                null);
    }

    /** Proiezioni di un film: sala e festival caricati (findByFilmIdWithSalaAndFestival). */
    public static ProiezioneDto perFilm(Proiezione p) {
        return new ProiezioneDto(p.getId(), p.getData(), p.getOra(), p.getOraFine(), p.getStato(),
                null,
                new Riferimento(p.getSala().getId(), p.getSala().getNome()),
                new Riferimento(p.getFestival().getId(), p.getFestival().getNome()));
    }
}
