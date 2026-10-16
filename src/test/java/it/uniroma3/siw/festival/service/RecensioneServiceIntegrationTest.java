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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
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
import it.uniroma3.siw.festival.repository.UtenteRepository;

/**
 * I metodi di scrittura richiedono che lo username passato sia quello dell'utente autenticato:
 * con @WithMockUser simulo di volta in volta l'utente che fa la richiesta.
 */
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
    private UtenteRepository utenteRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private Film film;

    @BeforeEach
    void setUp() {
        Regista regista = registaRepository.save(new Regista("Mario", "Rossi", LocalDate.of(1970, 1, 1), "Italiana"));
        film = filmRepository.save(new Film("Film di prova", 2020, 100, "Drammatico", "Italia", regista));
        utenteService.registra(new RegistrazioneForm("mario", "password"));
        utenteService.registra(new RegistrazioneForm("luigi", "password"));
    }

    @AfterEach
    void tearDown() {
        DatabaseCleaner.svuota(jdbc);
    }

    // Recensione di Mario inserita direttamente col repository (preparazione del test)
    private Recensione recensioneDiMario() {
        return recensioneRepository.save(new Recensione("Testo originale di Mario", 4, LocalDate.now(), film,
                utenteRepository.findByUsername("mario").orElseThrow()));
    }

    @Test
    @WithMockUser(username = "mario")
    void unaSolaRecensionePerUtentePerFilm() {
        recensioneService.crea(film.getId(), "mario", new RecensioneForm("Prima recensione del film", 4));

        assertThatThrownBy(() -> recensioneService.crea(film.getId(), "mario",
                new RecensioneForm("Ci riprovo con un'altra", 1)))
                .isInstanceOf(RecensioneDuplicataException.class);
        assertThat(recensioneRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "luigi")
    void unAltroUtentePuoRecensireLoStessoFilm() {
        recensioneDiMario();
        recensioneService.crea(film.getId(), "luigi", new RecensioneForm("Recensione di Luigi", 2));

        assertThat(recensioneService.recensioniDelFilm(film.getId())).hasSize(2);
        assertThat(recensioneService.mediaVoti(film.getId())).isEqualTo(3.0);
    }

    // Livello "codice": Luigi è autenticato e agisce a nome suo, ma la recensione non è sua → 403 di dominio
    @Test
    @WithMockUser(username = "luigi")
    void nonSiModificaNeSiEliminaLaRecensioneDiUnAltro() {
        Recensione r = recensioneDiMario();

        assertThatThrownBy(() -> recensioneService.modifica(r.getId(), "luigi",
                new RecensioneForm("Testo di Luigi!", 1)))
                .isInstanceOf(OperazioneNonAutorizzataException.class);
        assertThatThrownBy(() -> recensioneService.elimina(r.getId(), "luigi"))
                .isInstanceOf(OperazioneNonAutorizzataException.class);
        assertThat(recensioneRepository.findById(r.getId()).orElseThrow().getTesto())
                .isEqualTo("Testo originale di Mario");
    }

    @Test
    @WithMockUser(username = "mario")
    void lAutoreModificaEdEliminaLaPropria() {
        Recensione r = recensioneDiMario();

        recensioneService.modifica(r.getId(), "mario", new RecensioneForm("Testo corretto di Mario", 5));
        assertThat(recensioneRepository.findById(r.getId()).orElseThrow().getVoto()).isEqualTo(5);

        recensioneService.elimina(r.getId(), "mario");
        assertThat(recensioneRepository.count()).isZero();
    }

    // Livello "@PreAuthorize": Luigi prova a passare lo username di Mario → bloccato prima del metodo
    @Test
    @WithMockUser(username = "luigi")
    void nonSiAgisceANomeDiUnAltroUtente() {
        assertThatThrownBy(() -> recensioneService.crea(film.getId(), "mario",
                new RecensioneForm("Recensione falsificata", 5)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(recensioneRepository.count()).isZero();
    }

    @Test
    void senzaLoginNonSiRecensisce() {
        assertThatThrownBy(() -> recensioneService.crea(film.getId(), "mario",
                new RecensioneForm("Recensione anonima", 3)))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void registrazioneConUsernameGiaUsato() {
        assertThatThrownBy(() -> utenteService.registra(new RegistrazioneForm("mario", "altra")))
                .isInstanceOf(UsernameGiaUsatoException.class);
        assertThat(utenteService.trova("mario").getPassword()).startsWith("$2"); // hash BCrypt
    }
}
