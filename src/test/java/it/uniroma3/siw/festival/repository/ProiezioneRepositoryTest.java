package it.uniroma3.siw.festival.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.Sala;
import it.uniroma3.siw.festival.model.StatoProiezione;

// replace = NONE: usa il PostgreSQL configurato nel profilo test invece di un DB in memoria
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProiezioneRepositoryTest {

    private static final LocalDate GIORNO = LocalDate.of(2030, 5, 10);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProiezioneRepository proiezioneRepository;

    private Sala sala;
    private Sala altraSala;
    private Proiezione esistente; // dalle 14:00 alle 16:00

    @BeforeEach
    void setUp() {
        Regista regista = em.persist(new Regista("Mario", "Rossi", LocalDate.of(1970, 1, 1), "Italiana"));
        Film film = em.persist(new Film("Film di prova", 2020, 120, "Drammatico", "Italia", regista));
        Festival festival = new Festival("Festival di prova", 2030, "Roma", GIORNO, GIORNO.plusDays(5), null);
        festival.addFilm(film);
        em.persist(festival);
        sala = em.persist(new Sala("Sala 1", "Via Roma 1", 100));
        altraSala = em.persist(new Sala("Sala 2", "Via Roma 2", 100));

        esistente = new Proiezione();
        festival.addProiezione(esistente);
        film.addProiezione(esistente);
        sala.addProiezione(esistente);
        esistente.impostaOrario(GIORNO, LocalTime.of(14, 0));
        em.persist(esistente);
        em.flush();
    }

    private boolean sovrapposta(Sala s, String inizio, String fine, Long escludiId) {
        return proiezioneRepository.esisteSovrapposizione(s.getId(), GIORNO,
                LocalTime.parse(inizio), LocalTime.parse(fine), escludiId);
    }

    @Test
    void oraFineCalcolataDallaDurataDelFilm() {
        assertThat(esistente.getOraFine()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    void intervalloCheSiAccavallaESovrapposto() {
        assertThat(sovrapposta(sala, "15:00", "17:00", null)).isTrue();
        assertThat(sovrapposta(sala, "13:00", "14:30", null)).isTrue();
        assertThat(sovrapposta(sala, "14:30", "15:30", null)).isTrue(); // contenuto nell'esistente
    }

    @Test
    void intervalliAdiacentiNonSonoSovrapposti() {
        assertThat(sovrapposta(sala, "16:00", "18:00", null)).isFalse();
        assertThat(sovrapposta(sala, "12:00", "14:00", null)).isFalse();
    }

    @Test
    void altraSalaNonESovrapposta() {
        assertThat(sovrapposta(altraSala, "15:00", "17:00", null)).isFalse();
    }

    @Test
    void proiezioneAnnullataVieneIgnorata() {
        esistente.setStato(StatoProiezione.CANCELLED);
        em.flush();
        assertThat(sovrapposta(sala, "15:00", "17:00", null)).isFalse();
    }

    @Test
    void inModificaLaProiezioneStessaVieneEsclusa() {
        assertThat(sovrapposta(sala, "14:30", "16:30", esistente.getId())).isFalse();
    }
}
