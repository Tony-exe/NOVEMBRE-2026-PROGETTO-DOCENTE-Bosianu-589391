package it.uniroma3.siw.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import it.uniroma3.siw.festival.exception.ConflittoProgrammazioneException;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.Sala;
import it.uniroma3.siw.festival.model.StatoProiezione;
import it.uniroma3.siw.festival.repository.FestivalRepository;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;
import it.uniroma3.siw.festival.repository.RegistaRepository;
import it.uniroma3.siw.festival.repository.SalaRepository;

/**
 * Test di integrazione su PostgreSQL vero: transazioni, rollback e lock reali.
 * La classe NON è @Transactional, così ogni chiamata al service apre e chiude la propria transazione.
 */
@SpringBootTest
@WithMockUser(roles = "ADMIN") // i metodi di scrittura del service richiedono il ruolo ADMIN (@PreAuthorize)
@ActiveProfiles("test")
class ProiezioneServiceIntegrationTest {

    @Autowired
    private ProiezioneService proiezioneService;
    @Autowired
    private FestivalService festivalService;
    @Autowired
    private ProiezioneRepository proiezioneRepository;
    @Autowired
    private FestivalRepository festivalRepository;
    @Autowired
    private FilmRepository filmRepository;
    @Autowired
    private RegistaRepository registaRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbc;

    private Long festivalId;
    private Long filmId;
    private Long salaId;
    private LocalDate giorno;

    @BeforeEach
    void setUp() {
        LocalDate oggi = LocalDate.now();
        giorno = oggi.plusDays(12);
        Regista regista = registaRepository.save(new Regista("Mario", "Rossi", LocalDate.of(1970, 1, 1), "Italiana"));
        Film film = filmRepository.save(new Film("Film di prova", 2020, 120, "Drammatico", "Italia", regista));
        Festival festival = new Festival("Festival di prova", oggi.getYear(), "Roma", oggi.plusDays(10),
                oggi.plusDays(20), null);
        festival.addFilm(film);
        festivalId = festivalRepository.save(festival).getId();
        filmId = film.getId();
        salaId = salaRepository.save(new Sala("Sala 1", "Via Roma 1", 100)).getId();
    }

    @AfterEach
    void tearDown() {
        DatabaseCleaner.svuota(jdbc);
    }

    @Test
    void programmazioneValidaVieneConfermata() {
        proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno, LocalTime.of(14, 0));

        List<Proiezione> programma = proiezioneService.programma(festivalId, null);
        assertThat(programma).hasSize(1);
        assertThat(programma.get(0).getOraFine()).isEqualTo(LocalTime.of(16, 0));
        assertThat(programma.get(0).getSala().getNome()).isEqualTo("Sala 1"); // caricata col JOIN FETCH
    }

    @Test
    void programmazioneFallitaNonLasciaDatiParziali() {
        proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno, LocalTime.of(14, 0));

        assertThatThrownBy(() -> proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno,
                LocalTime.of(15, 0))).isInstanceOf(ConflittoProgrammazioneException.class);

        assertThat(proiezioneRepository.count()).isEqualTo(1);
    }

    /*
     * Rollback di un'unità di lavoro più ampia: due programmazioni nella stessa transazione esterna.
     * Con propagazione REQUIRED (default) i metodi del service si uniscono alla transazione esistente;
     * la seconda fallisce, la transazione viene marcata rollback-only e viene annullata per intero,
     * compresa la prima proiezione che era già stata inserita (INSERT eseguita, mai confermata).
     */
    @Test
    void seLaTransazioneFallisceVieneAnnullataAncheLaParteGiaEseguita() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno, LocalTime.of(10, 0));
            proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno, LocalTime.of(11, 0));
        })).isInstanceOf(ConflittoProgrammazioneException.class);

        assertThat(proiezioneRepository.count()).isZero();
    }

    /*
     * Due thread programmano nello stesso momento proiezioni sovrapposte nella stessa sala.
     * Grazie al lock pessimistico sulla sala, il secondo aspetta il commit del primo e poi trova
     * la sovrapposizione: esattamente una programmazione riesce, l'altra riceve un conflitto.
     */
    @Test
    void dueProgrammazioniConcorrentiNellaStessaSala() throws Exception {
        CountDownLatch via = new CountDownLatch(1);
        Callable<Proiezione> programma = () -> {
            via.await();
            return proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno, LocalTime.of(14, 0));
        };

        // Il SecurityContext è legato al thread: il wrapper lo copia nei thread del pool
        ExecutorService executor = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(2));
        List<Future<Proiezione>> risultati = new ArrayList<>();
        risultati.add(executor.submit(programma));
        risultati.add(executor.submit(programma));
        via.countDown(); // partono insieme

        int riuscite = 0;
        int conflitti = 0;
        for (Future<Proiezione> risultato : risultati) {
            try {
                risultato.get(30, TimeUnit.SECONDS);
                riuscite++;
            } catch (java.util.concurrent.ExecutionException e) {
                assertThat(e.getCause()).isInstanceOf(ConflittoProgrammazioneException.class);
                conflitti++;
            }
        }
        executor.shutdown();

        assertThat(riuscite).isEqualTo(1);
        assertThat(conflitti).isEqualTo(1);
        assertThat(proiezioneRepository.count()).isEqualTo(1);
    }

    @Test
    void rimozioneFilmDalFestivalAnnullaLeProiezioniProgrammate() {
        Proiezione programmata = proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno,
                LocalTime.of(14, 0));
        // una proiezione già svolta, inserita direttamente (il service non programma nel passato)
        Long svoltaId = transactionTemplate.execute(status -> {
            Proiezione svolta = new Proiezione();
            svolta.setFestival(festivalRepository.getReferenceById(festivalId));
            svolta.setFilm(filmRepository.findById(filmId).orElseThrow());
            svolta.setSala(salaRepository.getReferenceById(salaId));
            svolta.impostaOrario(giorno.minusDays(1), LocalTime.of(10, 0));
            svolta.setStato(StatoProiezione.COMPLETED);
            return proiezioneRepository.save(svolta).getId();
        });

        int annullate = festivalService.rimuoviFilm(festivalId, filmId);

        assertThat(annullate).isEqualTo(1);
        assertThat(festivalRepository.partecipaAlFestival(festivalId, filmId)).isFalse();
        assertThat(proiezioneRepository.findById(programmata.getId()).orElseThrow().getStato())
                .isEqualTo(StatoProiezione.CANCELLED);
        assertThat(proiezioneRepository.findById(svoltaId).orElseThrow().getStato())
                .isEqualTo(StatoProiezione.COMPLETED);
    }

    @Test
    @WithMockUser(roles = "USER") // sovrascrive l'annotazione di classe
    void unUtenteNonAdminNonPuoProgrammare() {
        assertThatThrownBy(() -> proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno,
                LocalTime.of(14, 0))).isInstanceOf(AccessDeniedException.class);
        assertThat(proiezioneRepository.count()).isZero();
    }

    @Test
    void lAnnullamentoLiberaLaSala() {
        Proiezione p = proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno, LocalTime.of(14, 0));
        proiezioneService.annulla(p.getId());

        // ora lo stesso orario è di nuovo disponibile
        proiezioneService.programmaProiezione(festivalId, filmId, salaId, giorno, LocalTime.of(14, 0));
        assertThat(proiezioneRepository.count()).isEqualTo(2);
    }
}
