package it.uniroma3.siw.festival.dto.api;

import java.time.LocalDate;

import it.uniroma3.siw.festival.model.Regista;

public record RegistaDto(Long id, String nome, String cognome, LocalDate dataNascita, String nazionalita) {

    public static RegistaDto da(Regista r) {
        return new RegistaDto(r.getId(), r.getNome(), r.getCognome(), r.getDataNascita(), r.getNazionalita());
    }
}
