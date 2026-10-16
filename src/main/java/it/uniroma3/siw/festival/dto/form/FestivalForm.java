package it.uniroma3.siw.festival.dto.form;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import it.uniroma3.siw.festival.model.Festival;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dati inseriti dall'amministratore per creare o modificare un festival.
 * Il form è separato dall'entità: il controller non lega mai i parametri HTTP direttamente
 * a un'entità JPA (si eviterebbe di sovrascrivere campi come id o collezioni).
 */
@PeriodoValido
public class FestivalForm {

    @NotBlank
    @Size(max = 255)
    private String nome;

    @NotNull
    @Min(1900)
    @Max(2100)
    private Integer anno;

    @NotBlank
    @Size(max = 255)
    private String citta;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInizio;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFine;

    @Size(max = 2000)
    private String descrizione;

    /** Precompila il form di modifica con i dati attuali. */
    public static FestivalForm da(Festival festival) {
        FestivalForm form = new FestivalForm();
        form.setNome(festival.getNome());
        form.setAnno(festival.getAnno());
        form.setCitta(festival.getCitta());
        form.setDataInizio(festival.getDataInizio());
        form.setDataFine(festival.getDataFine());
        form.setDescrizione(festival.getDescrizione());
        return form;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getAnno() {
        return anno;
    }

    public void setAnno(Integer anno) {
        this.anno = anno;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
}
