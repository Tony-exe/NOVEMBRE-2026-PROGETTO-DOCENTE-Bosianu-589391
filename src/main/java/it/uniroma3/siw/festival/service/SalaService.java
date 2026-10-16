package it.uniroma3.siw.festival.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw.festival.dto.form.SalaForm;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.model.Sala;
import it.uniroma3.siw.festival.repository.SalaRepository;

@Service
@Transactional(readOnly = true)
public class SalaService {

    private final SalaRepository salaRepository;

    public SalaService(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    public List<Sala> elenco() {
        return salaRepository.findAllByOrderByNomeAsc();
    }

    public Sala trova(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sala", id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Sala crea(SalaForm form) {
        Sala sala = new Sala();
        copiaDati(form, sala);
        return salaRepository.save(sala);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Sala modifica(Long id, SalaForm form) {
        Sala sala = trova(id);
        copiaDati(form, sala);
        return sala;
    }

    private void copiaDati(SalaForm form, Sala sala) {
        sala.setNome(form.getNome());
        sala.setIndirizzo(form.getIndirizzo());
        sala.setCapienza(form.getCapienza());
    }
}
