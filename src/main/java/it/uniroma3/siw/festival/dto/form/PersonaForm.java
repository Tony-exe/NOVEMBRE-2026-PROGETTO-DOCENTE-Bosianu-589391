package it.uniroma3.siw.festival.dto.form;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/** Form condiviso da Regista e Attore, che hanno gli stessi attributi. */
public class PersonaForm {

    @NotBlank
    @Size(max = 100)
    private String nome;

    @NotBlank
    @Size(max = 100)
    private String cognome;

    @Past
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataNascita;

    @NotBlank
    @Size(max = 100)
    private String nazionalita;

    public static PersonaForm da(String nome, String cognome, LocalDate dataNascita, String nazionalita) {
        PersonaForm form = new PersonaForm();
        form.setNome(nome);
        form.setCognome(cognome);
        form.setDataNascita(dataNascita);
        form.setNazionalita(nazionalita);
        return form;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(LocalDate dataNascita) {
        this.dataNascita = dataNascita;
    }

    public String getNazionalita() {
        return nazionalita;
    }

    public void setNazionalita(String nazionalita) {
        this.nazionalita = nazionalita;
    }
}
