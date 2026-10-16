package it.uniroma3.siw.festival.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import it.uniroma3.siw.festival.model.Attore;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Regista;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FilmRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private FilmRepository filmRepository;

    private Festival festival;
    private Film padrino;

    @BeforeEach
    void setUp() {
        Regista coppola = em.persist(new Regista("Francis Ford", "Coppola", LocalDate.of(1939, 4, 7), "Statunitense"));
        Regista fellini = em.persist(new Regista("Federico", "Fellini", LocalDate.of(1920, 1, 20), "Italiana"));
        Attore brando = em.persist(new Attore("Marlon", "Brando", LocalDate.of(1924, 4, 3), "Statunitense"));

        padrino = new Film("Il Padrino", 1972, 175, "Drammatico", "Stati Uniti", coppola);
        padrino.addInterpretazione(brando, "Don Vito Corleone");
        em.persist(padrino);
        Film dolceVita = em.persist(new Film("La dolce vita", 1960, 174, "Drammatico", "Italia", fellini));

        festival = new Festival("Venezia", 2030, "Venezia", LocalDate.of(2030, 9, 1), LocalDate.of(2030, 9, 10), null);
        festival.addFilm(padrino);
        festival.addFilm(dolceVita);
        em.persist(festival);

        // Svuoto il persistence context: le query successive caricano dal DB, non dalla cache di primo livello
        em.flush();
        em.clear();
    }

    @Test
    void senzaFetchIlRegistaRestaUnProxyNonInizializzato() {
        List<Film> films = filmRepository.findByFestivalId(festival.getId());
        assertThat(films).hasSize(2);
        assertThat(films).allMatch(f -> !Hibernate.isInitialized(f.getRegista()));
    }

    @Test
    void joinFetchCaricaIlRegista() {
        List<Film> films = filmRepository.findByFestivalIdWithRegista(festival.getId());
        assertThat(films).hasSize(2);
        assertThat(films).allMatch(f -> Hibernate.isInitialized(f.getRegista()));
    }

    @Test
    void entityGraphCaricaIlRegista() {
        List<Film> films = filmRepository.findByFestivalIdWithEntityGraph(festival.getId());
        assertThat(films).hasSize(2);
        assertThat(films).allMatch(f -> Hibernate.isInitialized(f.getRegista()));
    }

    @Test
    void dettaglioConRegistaECast() {
        Film film = filmRepository.findByIdWithRegistaAndCast(padrino.getId()).orElseThrow();
        assertThat(Hibernate.isInitialized(film.getRegista())).isTrue();
        assertThat(Hibernate.isInitialized(film.getInterpretazioni())).isTrue();
        assertThat(film.getInterpretazioni()).extracting(i -> i.getPersonaggio()).containsExactly("Don Vito Corleone");
    }

    @Test
    void ricercaPerRegistaConFiltriVuoti() {
        Page<Film> pagina = filmRepository.cerca("", "", "fellini", PageRequest.of(0, 10));
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getTitolo()).isEqualTo("La dolce vita");

        assertThat(filmRepository.cerca("", "", "", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
    }
}
