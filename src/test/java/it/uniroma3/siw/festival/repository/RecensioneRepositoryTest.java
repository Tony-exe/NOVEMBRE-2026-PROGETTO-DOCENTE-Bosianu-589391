package it.uniroma3.siw.festival.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Recensione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.Ruolo;
import it.uniroma3.siw.festival.model.Utente;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RecensioneRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RecensioneRepository recensioneRepository;

    private Film film;
    private Utente utente;
    private Utente altroUtente;

    @BeforeEach
    void setUp() {
        Regista regista = em.persist(new Regista("Mario", "Rossi", LocalDate.of(1970, 1, 1), "Italiana"));
        film = em.persist(new Film("Film di prova", 2020, 100, "Drammatico", "Italia", regista));
        utente = em.persist(new Utente("mario", "hash-finto", Ruolo.USER));
        altroUtente = em.persist(new Utente("luigi", "hash-finto", Ruolo.USER));
    }

    @Test
    void secondaRecensioneDelloStessoUtenteViolaIlVincoloUnique() {
        recensioneRepository.saveAndFlush(new Recensione("Prima recensione del film", 4, LocalDate.now(), film, utente));

        // Il vincolo unique (film_id, utente_id) è nel DB: l'INSERT fallisce e Spring traduce l'errore
        assertThatThrownBy(() -> recensioneRepository
                .saveAndFlush(new Recensione("Seconda recensione del film", 2, LocalDate.now(), film, utente)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mediaEdEsistenza() {
        recensioneRepository.save(new Recensione("Recensione positiva", 5, LocalDate.now(), film, utente));
        recensioneRepository.save(new Recensione("Recensione negativa", 2, LocalDate.now(), film, altroUtente));
        em.flush();

        assertThat(recensioneRepository.mediaVoti(film.getId())).isEqualTo(3.5);
        assertThat(recensioneRepository.existsByFilmIdAndUtenteId(film.getId(), utente.getId())).isTrue();
        assertThat(recensioneRepository.findByFilmIdWithUtente(film.getId())).hasSize(2);
    }
}
