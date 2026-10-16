package it.uniroma3.siw.festival.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw.festival.model.Regista;

public interface RegistaRepository extends JpaRepository<Regista, Long> {

    List<Regista> findAllByOrderByCognomeAscNomeAsc();
}
