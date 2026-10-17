package it.uniroma3.siw.festival.dto.api;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Una pagina di risultati. Non serializzo direttamente Page di Spring Data: il suo formato JSON
 * contiene molti dettagli interni e non è garantito stabile tra versioni.
 */
public record PaginaDto<T>(List<T> contenuto, int pagina, int dimensione, long totaleElementi, int totalePagine) {

    public static <E, T> PaginaDto<T> da(Page<E> page, Function<E, T> conversione) {
        return new PaginaDto<>(page.getContent().stream().map(conversione).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
