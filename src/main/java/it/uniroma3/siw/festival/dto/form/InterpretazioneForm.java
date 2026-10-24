package it.uniroma3.siw.festival.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Aggiunta di un attore al cast di un film, con il personaggio interpretato. */
public class InterpretazioneForm {

    @NotNull
    private Long attoreId;

    @NotBlank
    @Size(max = 255)
    private String personaggio;

    public Long getAttoreId() {
        return attoreId;
    }

    public void setAttoreId(Long attoreId) {
        this.attoreId = attoreId;
    }

    public String getPersonaggio() {
        return personaggio;
    }

    public void setPersonaggio(String personaggio) {
        this.personaggio = personaggio;
    }
}
