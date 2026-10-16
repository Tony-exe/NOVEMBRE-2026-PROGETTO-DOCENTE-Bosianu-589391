package it.uniroma3.siw.festival.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import it.uniroma3.siw.festival.model.Sala;
import jakarta.persistence.LockModeType;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    List<Sala> findAllByOrderByNomeAsc();

    /*
     * Lock pessimistico: genera "SELECT ... FOR UPDATE" su PostgreSQL. La riga della sala resta
     * bloccata fino alla fine della transazione, quindi due programmazioni concorrenti nella stessa
     * sala vengono serializzate: la seconda aspetta, e quando controlla le sovrapposizioni vede anche
     * la proiezione appena inserita dalla prima. Va chiamato dentro una transazione (service).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sala s where s.id = :id")
    Optional<Sala> findByIdForUpdate(Long id);
}
