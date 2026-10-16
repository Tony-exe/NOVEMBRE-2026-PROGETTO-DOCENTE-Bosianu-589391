package it.uniroma3.siw.festival.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw.festival.dto.form.PersonaForm;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.model.Attore;
import it.uniroma3.siw.festival.repository.AttoreRepository;

@Service
@Transactional(readOnly = true)
public class AttoreService {

    private final AttoreRepository attoreRepository;

    public AttoreService(AttoreRepository attoreRepository) {
        this.attoreRepository = attoreRepository;
    }

    public List<Attore> elenco() {
        return attoreRepository.findAllByOrderByCognomeAscNomeAsc();
    }

    public Attore trova(Long id) {
        return attoreRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attore", id));
    }

    @Transactional
    public Attore crea(PersonaForm form) {
        Attore attore = new Attore();
        copiaDati(form, attore);
        return attoreRepository.save(attore);
    }

    @Transactional
    public Attore modifica(Long id, PersonaForm form) {
        Attore attore = trova(id);
        copiaDati(form, attore);
        return attore;
    }

    private void copiaDati(PersonaForm form, Attore attore) {
        attore.setNome(form.getNome());
        attore.setCognome(form.getCognome());
        attore.setDataNascita(form.getDataNascita());
        attore.setNazionalita(form.getNazionalita());
    }
}
