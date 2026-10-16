package it.uniroma3.siw.festival.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw.festival.model.Attore;

public interface AttoreRepository extends JpaRepository<Attore, Long> {

    List<Attore> findAllByOrderByCognomeAscNomeAsc();
}
