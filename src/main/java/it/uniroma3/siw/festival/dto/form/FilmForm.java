package it.uniroma3.siw.festival.dto.form;

import it.uniroma3.siw.festival.model.Film;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Dati di un film inseriti dall'amministratore; il regista si sceglie tra quelli esistenti. */
public class FilmForm {

    @NotBlank
    @Size(max = 255)
    private String titolo;

    @NotNull
    @Min(1888)
    @Max(2100)
    private Integer anno;

    @NotNull
    @Min(1)
    @Max(600)
    private Integer durata;

    @NotBlank
    @Size(max = 100)
    private String genere;

    @NotBlank
    @Size(max = 100)
    private String paeseProduzione;

    @NotNull
    private Long registaId;

    public static FilmForm da(Film film) {
        FilmForm form = new FilmForm();
        form.setTitolo(film.getTitolo());
        form.setAnno(film.getAnno());
        form.setDurata(film.getDurata());
        form.setGenere(film.getGenere());
        form.setPaeseProduzione(film.getPaeseProduzione());
        form.setRegistaId(film.getRegista().getId());
        return form;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public Integer getAnno() {
        return anno;
    }

    public void setAnno(Integer anno) {
        this.anno = anno;
    }

    public Integer getDurata() {
        return durata;
    }

    public void setDurata(Integer durata) {
        this.durata = durata;
    }

    public String getGenere() {
        return genere;
    }

    public void setGenere(String genere) {
        this.genere = genere;
    }

    public String getPaeseProduzione() {
        return paeseProduzione;
    }

    public void setPaeseProduzione(String paeseProduzione) {
        this.paeseProduzione = paeseProduzione;
    }

    public Long getRegistaId() {
        return registaId;
    }

    public void setRegistaId(Long registaId) {
        this.registaId = registaId;
    }
}
