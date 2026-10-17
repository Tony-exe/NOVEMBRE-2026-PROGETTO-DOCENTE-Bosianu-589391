package it.uniroma3.siw.festival.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.Sala;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;
import it.uniroma3.siw.festival.support.DatabaseCleaner;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

/**
 * ESPERIMENTO SULLE STRATEGIE DI FETCH.
 * Si lancia con:  ./mvnw test -Dtest=FetchStrategyExperimentTest
 *
 * Confronta, sullo stesso insieme di dati, il numero di query SQL, il tempo e le entità caricate da
 * strategie diverse per lo stesso caso d'uso:
 *   Caso 1 - film di un festival con i relativi registi: LAZY, JOIN FETCH, @EntityGraph, batch fetch
 *   Caso 2 - programma di un festival con film e sale: LAZY, JOIN FETCH
 *
 * Regole per un confronto equo:
 * - ogni strategia gira in una NUOVA transazione con il persistence context vuoto (entityManager.clear()),
 *   così nessuna trova entità già caricate dalla precedente;
 * - prima delle misure c'è un giro di riscaldamento non conteggiato (JIT, connessioni del pool, cache
 *   dei piani di query di Hibernate);
 * - le Statistics di Hibernate vengono azzerate all'inizio di ogni misura.
 * I tempi includono anche la scrittura del log SQL: non è un benchmark rigoroso, contano gli ordini di grandezza.
 * Le asserzioni finali fissano il numero di query atteso: se qualcuno introduce un N+1, il test fallisce.
 */
@SpringBootTest
@ActiveProfiles("experiment")
class FetchStrategyExperimentTest {

    private static final int NUMERO_FILM = 100;
    private static final int NUMERO_SALE = 4;
    private static final int PROIEZIONI_PER_FILM = 3;
    private static final int DIMENSIONE_BATCH = 25;

    @Autowired
    private FilmRepository filmRepository;
    @Autowired
    private ProiezioneRepository proiezioneRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private JdbcTemplate jdbc;

    // Proxy dell'EntityManager legato alla transazione corrente
    @PersistenceContext
    private EntityManager entityManager;

    private Statistics statistics;
    private Long festivalId;

    /** Risultato di una misura. */
    private record Risultato(String strategia, int oggetti, long query, long entitaCaricate, long millisecondi) {
    }

    @BeforeEach
    void preparaDati() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        festivalId = transactionTemplate.execute(status -> generaDataset());
    }

    @AfterEach
    void pulisci() {
        DatabaseCleaner.svuota(jdbc);
    }

    // =====================================================================================
    // CASO 1: film di un festival con i relativi registi
    // =====================================================================================

    @Test
    void filmDelFestivalConRegisti() {
        // Strategia 1 - LAZY: una query per i film, poi una per ogni regista al primo accesso (N+1)
        Supplier<Integer> lazy = () -> accediAiRegisti(filmRepository.findByFestivalId(festivalId));
        // Strategia 2 - JOIN FETCH: film e registi in un'unica SELECT con join
        Supplier<Integer> joinFetch = () -> accediAiRegisti(filmRepository.findByFestivalIdWithRegista(festivalId));
        // Strategia 3 - @EntityGraph: stessa query della 1, ma il grafo chiede di caricare anche il regista
        Supplier<Integer> entityGraph = () -> accediAiRegisti(filmRepository.findByFestivalIdWithEntityGraph(festivalId));
        // Strategia 4 - batch fetch: query LAZY, ma Hibernate carica i proxy dei registi a gruppi di 25
        // (stesso effetto di @BatchSize sull'entità o di hibernate.default_batch_fetch_size, ma solo per questa sessione)
        Supplier<Integer> batch = () -> {
            entityManager.unwrap(Session.class).setFetchBatchSize(DIMENSIONE_BATCH);
            return accediAiRegisti(filmRepository.findByFestivalId(festivalId));
        };

        riscaldamento(lazy, joinFetch, entityGraph, batch);

        System.out.println();
        System.out.println("=== Test accesso ai film del festival ===");
        Risultato r1 = stampa(1, misura("LAZY", lazy), "Film caricati");
        Risultato r2 = stampa(2, misura("JOIN FETCH", joinFetch), "Film caricati");
        Risultato r3 = stampa(3, misura("@EntityGraph", entityGraph), "Film caricati");
        Risultato r4 = stampa(4, misura("Batch fetch (" + DIMENSIONE_BATCH + ")", batch), "Film caricati");

        assertThat(r1.query()).isEqualTo(1 + NUMERO_FILM);                              // 1 + N
        assertThat(r2.query()).isEqualTo(1);
        assertThat(r3.query()).isEqualTo(1);
        assertThat(r4.query()).isEqualTo(1 + NUMERO_FILM / DIMENSIONE_BATCH);           // 1 + N/25
        List.of(r1, r2, r3, r4).forEach(r -> {
            assertThat(r.oggetti()).isEqualTo(NUMERO_FILM);
            assertThat(r.entitaCaricate()).isEqualTo(2L * NUMERO_FILM);                 // 100 film + 100 registi
        });
    }

    /** Il "caso d'uso": per ogni film leggo il cognome del regista (come farebbe una pagina). */
    private int accediAiRegisti(List<Film> films) {
        int letti = 0;
        for (Film film : films) {
            if (film.getRegista().getCognome() != null) { // con LAZY qui parte una query per ogni regista
                letti++;
            }
        }
        return letti;
    }

    // =====================================================================================
    // CASO 2: programma di un festival con film e sale
    // =====================================================================================

    @Test
    void programmaDelFestivalConFilmESale() {
        // LAZY: 1 query per le proiezioni + 1 per ogni film distinto + 1 per ogni sala distinta
        Supplier<Integer> lazy = () -> accediAFilmESale(proiezioneRepository.findByFestivalIdOrderByDataAscOraAsc(festivalId));
        // JOIN FETCH su due associazioni to-one: un'unica query, nessun prodotto cartesiano
        Supplier<Integer> joinFetch = () -> accediAFilmESale(proiezioneRepository.findProgramma(festivalId));

        riscaldamento(lazy, joinFetch);

        System.out.println();
        System.out.println("=== Test programma del festival (proiezioni con film e sale) ===");
        Risultato r1 = stampa(1, misura("LAZY", lazy), "Proiezioni caricate");
        Risultato r2 = stampa(2, misura("JOIN FETCH", joinFetch), "Proiezioni caricate");

        int proiezioni = NUMERO_FILM * PROIEZIONI_PER_FILM;
        assertThat(r1.query()).isEqualTo(1 + NUMERO_FILM + NUMERO_SALE);
        assertThat(r2.query()).isEqualTo(1);
        assertThat(r1.oggetti()).isEqualTo(proiezioni);
        assertThat(r2.oggetti()).isEqualTo(proiezioni);
        assertThat(r2.entitaCaricate()).isEqualTo(proiezioni + NUMERO_FILM + NUMERO_SALE);
    }

    private int accediAFilmESale(List<Proiezione> proiezioni) {
        int lette = 0;
        for (Proiezione p : proiezioni) {
            if (p.getFilm().getTitolo() != null && p.getSala().getNome() != null) {
                lette++;
            }
        }
        return lette;
    }

    // =====================================================================================
    // Misura e stampa
    // =====================================================================================

    /** Esegue la strategia in una nuova transazione, con persistence context vuoto e statistiche azzerate. */
    private Risultato misura(String nome, Supplier<Integer> strategia) {
        return transactionTemplate.execute(status -> {
            entityManager.clear();  // nessuna entità già in memoria
            statistics.clear();     // contatori a zero
            long inizio = System.nanoTime();
            int oggetti = strategia.get();
            long millisecondi = (System.nanoTime() - inizio) / 1_000_000;
            return new Risultato(nome, oggetti, statistics.getPrepareStatementCount(),
                    statistics.getEntityLoadCount(), millisecondi);
        });
    }

    @SafeVarargs
    private void riscaldamento(Supplier<Integer>... strategie) {
        for (Supplier<Integer> strategia : strategie) {
            misura("riscaldamento", strategia);
        }
    }

    private Risultato stampa(int numero, Risultato r, String etichetta) {
        System.out.println();
        System.out.println("Strategia " + numero + ": " + r.strategia());
        System.out.println(etichetta + ": " + r.oggetti());
        System.out.println("Query SQL: " + r.query());
        System.out.println("Oggetti caricati in totale: " + r.entitaCaricate());
        System.out.println("Tempo: " + r.millisecondi() + " ms");
        return r;
    }

    // =====================================================================================
    // Dataset: 1 festival, 100 film con registi tutti diversi, 4 sale, 3 proiezioni per film
    // =====================================================================================

    private Long generaDataset() {
        LocalDate inizio = LocalDate.of(2030, 9, 1);
        Festival festival = new Festival("Festival dell'esperimento", 2030, "Roma", inizio, inizio.plusDays(9), null);

        List<Sala> sale = new ArrayList<>();
        for (int s = 1; s <= NUMERO_SALE; s++) {
            Sala sala = new Sala("Sala " + s, "Via dell'esperimento " + s, 200);
            entityManager.persist(sala);
            sale.add(sala);
        }

        List<Film> films = new ArrayList<>();
        for (int i = 1; i <= NUMERO_FILM; i++) {
            Regista regista = new Regista("Nome" + i, "Cognome" + i, LocalDate.of(1950 + i % 50, 1, 1), "Italiana");
            entityManager.persist(regista);
            Film film = new Film("Film " + i, 2000 + i % 25, 90 + i % 30, "Drammatico", "Italia", regista);
            entityManager.persist(film);
            festival.addFilm(film);
            films.add(film);
        }
        entityManager.persist(festival);

        // 3 proiezioni per film, distribuite su giorni, fasce orarie (fino a 2h di durata) e sale
        LocalTime[] fasce = { LocalTime.of(10, 0), LocalTime.of(12, 30), LocalTime.of(15, 0),
                LocalTime.of(17, 30), LocalTime.of(20, 0) };
        int k = 0;
        for (Film film : films) {
            for (int n = 0; n < PROIEZIONI_PER_FILM; n++, k++) {
                Proiezione p = new Proiezione();
                festival.addProiezione(p);
                film.addProiezione(p);
                sale.get(k % NUMERO_SALE).addProiezione(p);
                p.impostaOrario(inizio.plusDays((k / NUMERO_SALE) % 10), fasce[(k / (NUMERO_SALE * 10)) % fasce.length]);
                entityManager.persist(p);
            }
        }
        return festival.getId();
    }
}
