package it.uniroma3.siw.festival.dto.api;

import java.time.LocalDate;

import it.uniroma3.siw.festival.model.Festival;

/**
 * DTO JSON di un festival. Le API non restituiscono mai entità JPA: si evitano cicli nelle associazioni
 * bidirezionali, accessi LAZY fuori transazione durante la serializzazione e l'esposizione di campi interni.
 * Il "contratto" JSON resta stabile anche se cambia il modello.
 */
public record FestivalDto(Long id, String nome, Integer anno, String citta, LocalDate dataInizio,
        LocalDate dataFine, String descrizione) {

    public static FestivalDto da(Festival f) {
        return new FestivalDto(f.getId(), f.getNome(), f.getAnno(), f.getCitta(), f.getDataInizio(),
                f.getDataFine(), f.getDescrizione());
    }
}
