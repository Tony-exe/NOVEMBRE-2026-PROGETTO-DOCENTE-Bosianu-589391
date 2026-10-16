package it.uniroma3.siw.festival.dto.form;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/**
 * Programmazione o modifica di una proiezione. Il festival arriva dall'URL; in modifica il film
 * non cambia (si cambiano solo sala, data e ora), quindi filmId viene ignorato.
 * Le regole che dipendono dal DB (periodo del festival, sovrapposizioni...) sono nel service.
 */
public class ProiezioneForm {

    @NotNull(message = "Seleziona un film")
    private Long filmId;

    @NotNull(message = "Seleziona una sala")
    private Long salaId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime ora;

    public Long getFilmId() {
        return filmId;
    }

    public void setFilmId(Long filmId) {
        this.filmId = filmId;
    }

    public Long getSalaId() {
        return salaId;
    }

    public void setSalaId(Long salaId) {
        this.salaId = salaId;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getOra() {
        return ora;
    }

    public void setOra(LocalTime ora) {
        this.ora = ora;
    }
}
