package it.uniroma3.siw.festival.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.uniroma3.siw.festival.model.Festival;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    List<Festival> findAllByOrderByDataInizioDesc();

    // Festival con i suoi film in un'unica query (una sola collezione in JOIN FETCH)
    @Query("select fe from Festival fe left join fetch fe.films where fe.id = :id")
    Optional<Festival> findByIdWithFilms(Long id);

    // Festival a cui partecipa un film (per il dettaglio film): si parte dal lato proprietario
    @Query("select fe from Festival fe join fe.films f where f.id = :filmId order by fe.dataInizio")
    List<Festival> findByFilmId(Long filmId);

    // Verifica la partecipazione senza caricare la collezione dei film
    @Query("select count(fe) > 0 from Festival fe join fe.films f where fe.id = :festivalId and f.id = :filmId")
    boolean partecipaAlFestival(Long festivalId, Long filmId);
}
