package it.uniroma3.siw.festival.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw.festival.model.Sala;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    List<Sala> findAllByOrderByNomeAsc();
}
