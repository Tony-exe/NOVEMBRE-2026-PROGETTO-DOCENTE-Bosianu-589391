package it.uniroma3.siw.festival.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.uniroma3.siw.festival.model.Film;

public interface FilmRepository extends JpaRepository<Film, Long> {

    /*
     * Tre modi di caricare "i film di un festival" (confrontati nell'esperimento sulle strategie di fetch).
     */

    // 1) Solo i film: il regista resta un proxy LAZY. Se poi accedo a film.getRegista().getCognome()
    //    in un ciclo, Hibernate esegue una query per ogni regista → problema N+1 (1 + N query).
    @Query("select f from Film f join f.festivals fe where fe.id = :festivalId order by f.titolo")
    List<Film> findByFestivalId(Long festivalId);

    // 2) JOIN FETCH: film e registi in un'unica SELECT con join (1 query).
    @Query("select f from Film f join fetch f.regista join f.festivals fe where fe.id = :festivalId order by f.titolo")
    List<Film> findByFestivalIdWithRegista(Long festivalId);

    // 3) Stessa query della 1, ma l'@EntityGraph dice a Hibernate di caricare anche "regista":
    //    l'SQL è equivalente al JOIN FETCH, ma la strategia è dichiarata fuori dalla query.
    @EntityGraph(attributePaths = "regista")
    @Query("select f from Film f join f.festivals fe where fe.id = :festivalId order by f.titolo")
    List<Film> findByFestivalIdWithEntityGraph(Long festivalId);

    // Dettaglio film: regista + cast. Una sola collezione in fetch: festival, proiezioni e
    // recensioni si caricano con query separate per evitare il prodotto cartesiano.
    @Query("""
            select f from Film f
            join fetch f.regista
            left join fetch f.interpretazioni i
            left join fetch i.attore
            where f.id = :id
            """)
    Optional<Film> findByIdWithRegistaAndCast(Long id);

    /*
     * Ricerca paginata. I filtri assenti vanno passati come stringa vuota (like '%%' = tutto):
     * evito "(:param is null or ...)" che con PostgreSQL può dare problemi di tipo sui parametri null.
     * Il JOIN FETCH sul regista è compatibile con la paginazione perché è una to-one (non moltiplica
     * le righe); la countQuery è separata perché in una count non si può fare fetch.
     */
    @Query(value = """
            select f from Film f join fetch f.regista r
            where lower(f.titolo) like lower(concat('%', :titolo, '%'))
              and lower(f.genere) like lower(concat('%', :genere, '%'))
              and lower(concat(r.nome, ' ', r.cognome)) like lower(concat('%', :regista, '%'))
            """,
            countQuery = """
            select count(f) from Film f join f.regista r
            where lower(f.titolo) like lower(concat('%', :titolo, '%'))
              and lower(f.genere) like lower(concat('%', :genere, '%'))
              and lower(concat(r.nome, ' ', r.cognome)) like lower(concat('%', :regista, '%'))
            """)
    Page<Film> cerca(String titolo, String genere, String regista, Pageable pageable);

    // Film che si possono ancora associare al festival (quelli che non vi partecipano), con regista
    @Query("""
            select f from Film f join fetch f.regista
            where f.id not in (select fi.id from Festival fe join fe.films fi where fe.id = :festivalId)
            order by f.titolo
            """)
    List<Film> findNonPartecipantiAlFestival(Long festivalId);

    // Elenco admin: mostra anche il regista, quindi lo carico subito (altrimenti N+1)
    @Query("select f from Film f join fetch f.regista order by f.titolo")
    List<Film> findAllWithRegista();
}
