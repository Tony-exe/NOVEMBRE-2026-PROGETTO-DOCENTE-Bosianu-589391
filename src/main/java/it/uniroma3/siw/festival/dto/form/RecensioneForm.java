package it.uniroma3.siw.festival.dto.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Testo e voto di una recensione. Lo usa anche l'API REST come corpo JSON di POST/PUT:
 * film e autore non sono nel form (arrivano dall'URL e dall'utente autenticato),
 * così il client non può scrivere una recensione a nome di un altro.
 * I testi degli errori non sono nelle annotazioni ma in messages/messages.properties (es. "Size.testo").
 */
public class RecensioneForm {

    @NotBlank
    @Size(min = 10, max = 2000)
    private String testo;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer voto;

    public RecensioneForm() {
    }

    public RecensioneForm(String testo, Integer voto) {
        this.testo = testo;
        this.voto = voto;
    }

    public String getTesto() {
        return testo;
    }

    public void setTesto(String testo) {
        this.testo = testo;
    }

    public Integer getVoto() {
        return voto;
    }

    public void setVoto(Integer voto) {
        this.voto = voto;
    }
}
