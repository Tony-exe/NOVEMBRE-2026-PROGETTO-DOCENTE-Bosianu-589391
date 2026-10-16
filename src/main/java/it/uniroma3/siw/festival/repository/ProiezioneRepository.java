package it.uniroma3.siw.festival.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.StatoProiezione;

public interface ProiezioneRepository extends JpaRepository<Proiezione, Long> {

    // Programma di un festival SENZA fetch di film e sala (versione LAZY, usata nell'esperimento)
    List<Proiezione> findByFestivalIdOrderByDataAscOraAsc(Long festivalId);

    // Programma di un festival con film e sala in un'unica query (due to-one: nessun prodotto cartesiano)
    @Query("""
            select p from Proiezione p
            join fetch p.film
            join fetch p.sala
            where p.festival.id = :festivalId
            order by p.data, p.ora
            """)
    List<Proiezione> findProgramma(Long festivalId);

    @Query("""
            select p from Proiezione p
            join fetch p.film
            join fetch p.sala
            where p.festival.id = :festivalId and p.data = :data
            order by p.ora
            """)
    List<Proiezione> findProgrammaDelGiorno(Long festivalId, LocalDate data);

    // Proiezioni di un film (dettaglio film) con sala e festival
    @Query("""
            select p from Proiezione p
            join fetch p.sala
            join fetch p.festival
            where p.film.id = :filmId
            order by p.data, p.ora
            """)
    List<Proiezione> findByFilmIdWithSalaAndFestival(Long filmId);

    // Una proiezione con tutto ciò che serve per modificarla o mostrarla
    @Query("""
            select p from Proiezione p
            join fetch p.festival
            join fetch p.film
            join fetch p.sala
            where p.id = :id
            """)
    Optional<Proiezione> findByIdWithDettagli(Long id);

    boolean existsByFilmIdAndStato(Long filmId, StatoProiezione stato);

    // Proiezioni programmate che resterebbero fuori dal periodo se il festival venisse modificato
    @Query("""
            select count(p) > 0 from Proiezione p
            where p.festival.id = :festivalId
              and p.stato = it.uniroma3.siw.festival.model.StatoProiezione.SCHEDULED
              and (p.data < :inizio or p.data > :fine)
            """)
    boolean esistonoProiezioniFuoriPeriodo(Long festivalId, LocalDate inizio, LocalDate fine);

    // Proiezioni di un film in un festival (per la rimozione del film dal festival)
    @Query("select p from Proiezione p where p.festival.id = :festivalId and p.film.id = :filmId")
    List<Proiezione> findByFestivalIdAndFilmId(Long festivalId, Long filmId);

    /*
     * Due intervalli [inizio, fine) si sovrappongono se: inizio < altraFine AND fine > altraInizio.
     * Si confrontano solo le proiezioni non annullate della stessa sala nello stesso giorno.
     * escludiId: in modifica si esclude la proiezione stessa (null in inserimento).
     * Gli estremi sono esclusi: una proiezione può iniziare esattamente quando finisce la precedente.
     */
    @Query("""
            select count(p) > 0 from Proiezione p
            where p.sala.id = :salaId
              and p.data = :data
              and p.stato <> it.uniroma3.siw.festival.model.StatoProiezione.CANCELLED
              and p.ora < :oraFine
              and p.oraFine > :ora
              and (:escludiId is null or p.id <> :escludiId)
            """)
    boolean esisteSovrapposizione(Long salaId, LocalDate data, LocalTime ora, LocalTime oraFine, Long escludiId);
}
