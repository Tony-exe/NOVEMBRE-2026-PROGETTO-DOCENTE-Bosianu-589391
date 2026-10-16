package it.uniroma3.siw.festival.dto.form;

import it.uniroma3.siw.festival.model.Sala;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SalaForm {

    @NotBlank
    @Size(max = 255)
    private String nome;

    @NotBlank
    @Size(max = 255)
    private String indirizzo;

    @NotNull
    @Min(value = 1, message = "La capienza deve essere maggiore di zero")
    private Integer capienza;

    public static SalaForm da(Sala sala) {
        SalaForm form = new SalaForm();
        form.setNome(sala.getNome());
        form.setIndirizzo(sala.getIndirizzo());
        form.setCapienza(sala.getCapienza());
        return form;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public Integer getCapienza() {
        return capienza;
    }

    public void setCapienza(Integer capienza) {
        this.capienza = capienza;
    }
}
