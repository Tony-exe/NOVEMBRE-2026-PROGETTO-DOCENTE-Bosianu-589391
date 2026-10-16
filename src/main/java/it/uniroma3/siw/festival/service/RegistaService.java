package it.uniroma3.siw.festival.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw.festival.dto.form.PersonaForm;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.repository.RegistaRepository;

@Service
@Transactional(readOnly = true)
public class RegistaService {

    private final RegistaRepository registaRepository;

    public RegistaService(RegistaRepository registaRepository) {
        this.registaRepository = registaRepository;
    }

    public List<Regista> elenco() {
        return registaRepository.findAllByOrderByCognomeAscNomeAsc();
    }

    public Regista trova(Long id) {
        return registaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Regista", id));
    }

    @Transactional
    public Regista crea(PersonaForm form) {
        Regista regista = new Regista();
        copiaDati(form, regista);
        return registaRepository.save(regista);
    }

    @Transactional
    public Regista modifica(Long id, PersonaForm form) {
        Regista regista = trova(id);
        copiaDati(form, regista);
        return regista;
    }

    private void copiaDati(PersonaForm form, Regista regista) {
        regista.setNome(form.getNome());
        regista.setCognome(form.getCognome());
        regista.setDataNascita(form.getDataNascita());
        regista.setNazionalita(form.getNazionalita());
    }
}
