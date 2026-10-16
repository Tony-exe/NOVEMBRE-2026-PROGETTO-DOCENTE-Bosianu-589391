package it.uniroma3.siw.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import it.uniroma3.siw.festival.dto.form.RecensioneForm;
import it.uniroma3.siw.festival.dto.form.RegistrazioneForm;
import it.uniroma3.siw.festival.exception.OperazioneNonAutorizzataException;
import it.uniroma3.siw.festival.exception.RecensioneDuplicataException;
import it.uniroma3.siw.festival.exception.UsernameGiaUsatoException;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Recensione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.RecensioneRepository;
import it.uniroma3.siw.festival.repository.RegistaRepository;

@SpringBootTest
@ActiveProfiles("test")
class RecensioneServiceIntegrationTest {

    @Autowired
    private RecensioneService recensioneService;
    @Autowired
    private UtenteService utenteService;
    @Autowired
    private RecensioneRepository recensioneRepository;
    @Autowired
    private FilmRepository filmRepository;
    @Autowired
    private RegistaRepository registaRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private Long filmId;

    @BeforeEach
    void setUp() {
        Regista regista = registaRepository.save(new Regista("Mario", "Rossi", LocalDate.of(1970, 1, 1), "Italiana"));
        filmId = filmRepository.save(new Film("Film di prova", 2020, 100, "Drammatico", "Italia", regista)).getId();
        utenteService.registra(new RegistrazioneForm("mario", "password"));
        utenteService.registra(new RegistrazioneForm("luigi", "password"));
    }

    @AfterEach
    void tearDown() {
        DatabaseCleaner.svuota(jdbc);
    }

    @Test
    void unaSolaRecensionePerUtentePerFilm() {
        recensioneService.crea(filmId, "mario", new RecensioneForm("Prima recensione del film", 4));

        assertThatThrownBy(() -> recensioneService.crea(filmId, "mario", new RecensioneForm("Ci riprovo con un'altra", 1)))
                .isInstanceOf(RecensioneDuplicataException.class);

        // un altro utente invece può recensire lo stesso film
        recensioneService.crea(filmId, "luigi", new RecensioneForm("Recensione di Luigi", 2));
        assertThat(recensioneService.recensioniDelFilm(filmId)).hasSize(2);
        assertThat(recensioneService.mediaVoti(filmId)).isEqualTo(3.0);
    }

    @Test
    void soloLAutoreModificaEdElimina() {
        Recensione r = recensioneService.crea(filmId, "mario", new RecensioneForm("Testo originale di Mario", 4));

        assertThatThrownBy(() -> recensioneService.modifica(r.getId(), "luigi", new RecensioneForm("Testo di Luigi!", 1)))
                .isInstanceOf(OperazioneNonAutorizzataException.class);
        assertThatThrownBy(() -> recensioneService.elimina(r.getId(), "luigi"))
                .isInstanceOf(OperazioneNonAutorizzataException.class);
        assertThat(recensioneRepository.findById(r.getId()).orElseThrow().getTesto())
                .isEqualTo("Testo originale di Mario");

        recensioneService.modifica(r.getId(), "mario", new RecensioneForm("Testo corretto di Mario", 5));
        assertThat(recensioneRepository.findById(r.getId()).orElseThrow().getVoto()).isEqualTo(5);

        recensioneService.elimina(r.getId(), "mario");
        assertThat(recensioneRepository.count()).isZero();
    }

    @Test
    void registrazioneConUsernameGiaUsato() {
        assertThatThrownBy(() -> utenteService.registra(new RegistrazioneForm("mario", "altra")))
                .isInstanceOf(UsernameGiaUsatoException.class);
        assertThat(utenteService.trova("mario").getPassword()).startsWith("$2"); // hash BCrypt
    }
}
