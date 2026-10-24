package it.uniroma3.siw.festival.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import it.uniroma3.siw.festival.model.Attore;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Recensione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.Ruolo;
import it.uniroma3.siw.festival.model.Sala;
import it.uniroma3.siw.festival.model.Utente;
import it.uniroma3.siw.festival.repository.AttoreRepository;
import it.uniroma3.siw.festival.repository.FestivalRepository;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;
import it.uniroma3.siw.festival.repository.RecensioneRepository;
import it.uniroma3.siw.festival.repository.RegistaRepository;
import it.uniroma3.siw.festival.repository.SalaRepository;
import it.uniroma3.siw.festival.repository.UtenteRepository;
import it.uniroma3.siw.festival.support.DatabaseCleaner;

/**
 * Test delle API REST: codici HTTP, struttura del JSON e formato ProblemDetail degli errori.
 * Gli utenti "mario" e "luigi" esistono nel DB perché il service cerca l'autore per username;
 * @WithMockUser simula la loro sessione.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiRestTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RegistaRepository registaRepository;
    @Autowired
    private AttoreRepository attoreRepository;
    @Autowired
    private FilmRepository filmRepository;
    @Autowired
    private FestivalRepository festivalRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private ProiezioneRepository proiezioneRepository;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private RecensioneRepository recensioneRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private Long festivalId;
    private Long filmId;
    private Long recensioneDiLuigiId;
    private LocalDate giorno;

    @BeforeEach
    void setUp() {
        LocalDate oggi = LocalDate.now();
        giorno = oggi.plusDays(12);
        Regista coppola = registaRepository.save(new Regista("Francis Ford", "Coppola", LocalDate.of(1939, 4, 7), "Statunitense"));
        Attore brando = attoreRepository.save(new Attore("Marlon", "Brando", LocalDate.of(1924, 4, 3), "Statunitense"));
        Film padrino = new Film("Il Padrino", 1972, 120, "Drammatico", "Stati Uniti", coppola);
        padrino.addInterpretazione(brando, "Don Vito Corleone");
        filmRepository.save(padrino);
        Festival festival = new Festival("Festa del Cinema", oggi.getYear(), "Roma", oggi.plusDays(10), oggi.plusDays(20), null);
        festival.addFilm(padrino);
        festivalRepository.save(festival);
        Sala sala = salaRepository.save(new Sala("Sala Sinopoli", "Viale de Coubertin 30", 1200));
        for (int i = 0; i < 2; i++) {
            Proiezione p = new Proiezione();
            festival.addProiezione(p);
            padrino.addProiezione(p);
            sala.addProiezione(p);
            p.impostaOrario(giorno.plusDays(i), LocalTime.of(18, 0));
            proiezioneRepository.save(p);
        }
        utenteRepository.save(new Utente("mario", "hash", Ruolo.USER));
        Utente luigi = utenteRepository.save(new Utente("luigi", "hash", Ruolo.USER));
        recensioneDiLuigiId = recensioneRepository.save(
                new Recensione("Recensione scritta da Luigi", 2, oggi, padrino, luigi)).getId();

        festivalId = festival.getId();
        filmId = padrino.getId();
    }

    @AfterEach
    void tearDown() {
        DatabaseCleaner.svuota(jdbc);
    }

    // --- Letture pubbliche ---

    @Test
    void elencoEDettaglioFestival() throws Exception {
        mvc.perform(get("/api/festivals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Festa del Cinema"));
        mvc.perform(get("/api/festivals/{id}", festivalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citta").value("Roma"));
    }

    @Test
    void festivalInesistenteRestituisce404ProblemDetail() throws Exception {
        mvc.perform(get("/api/festivals/{id}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Risorsa non trovata"))
                .andExpect(jsonPath("$.detail").value("Festival con id 99999 non trovato"));
    }

    @Test
    void filmDelFestivalConRegista() throws Exception {
        mvc.perform(get("/api/festivals/{id}/movies", festivalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titolo").value("Il Padrino"))
                .andExpect(jsonPath("$[0].regista.cognome").value("Coppola"));
    }

    @Test
    void programmaConEFiltroPerData() throws Exception {
        mvc.perform(get("/api/festivals/{id}/screenings", festivalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].film.nome").value("Il Padrino"))
                .andExpect(jsonPath("$[0].sala.nome").value("Sala Sinopoli"))
                .andExpect(jsonPath("$[0].ora").value("18:00:00"))
                .andExpect(jsonPath("$[0].festival").doesNotExist()); // campo null non serializzato

        mvc.perform(get("/api/festivals/{id}/screenings", festivalId).param("date", giorno.toString()))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void dataMalformataRestituisce400() throws Exception {
        mvc.perform(get("/api/festivals/{id}/screenings", festivalId).param("date", "ieri"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void ricercaPaginataPerRegista() throws Exception {
        mvc.perform(get("/api/movies").param("director", "copp").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaleElementi").value(1))
                .andExpect(jsonPath("$.dimensione").value(5))
                .andExpect(jsonPath("$.contenuto[0].titolo").value("Il Padrino"));
        mvc.perform(get("/api/movies").param("title", "inesistente"))
                .andExpect(jsonPath("$.totaleElementi").value(0));
    }

    @Test
    void dettaglioFilmCompleto() throws Exception {
        mvc.perform(get("/api/movies/{id}", filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regista.nome").value("Francis Ford"))
                .andExpect(jsonPath("$.cast[0].personaggio").value("Don Vito Corleone"))
                .andExpect(jsonPath("$.festivals[0].nome").value("Festa del Cinema"))
                .andExpect(jsonPath("$.proiezioni", hasSize(2)))
                .andExpect(jsonPath("$.proiezioni[0].festival.nome").value("Festa del Cinema"))
                .andExpect(jsonPath("$.mediaVoti").value(2.0))
                .andExpect(jsonPath("$.numeroRecensioni").value(1));
    }

    @Test
    void recensioniDelFilm() throws Exception {
        mvc.perform(get("/api/movies/{id}/reviews", filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(1))
                .andExpect(jsonPath("$.recensioni[0].autore").value("luigi"))
                .andExpect(jsonPath("$.recensioni[0].password").doesNotExist());
    }

    // --- Scritture sulle recensioni ---

    @Test
    @WithMockUser(username = "mario")
    void inserimentoRestituisce201ConLocation() throws Exception {
        mvc.perform(post("/api/movies/{id}/reviews", filmId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testo\": \"Capolavoro senza tempo\", \"voto\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/reviews/\\d+")))
                .andExpect(jsonPath("$.autore").value("mario"))
                .andExpect(jsonPath("$.voto").value(5));
    }

    @Test
    @WithMockUser(username = "luigi")
    void secondaRecensioneRestituisce409() throws Exception {
        mvc.perform(post("/api/movies/{id}/reviews", filmId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testo\": \"Ci riprovo un'altra volta\", \"voto\": 4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Hai già recensito")));
    }

    @Test
    @WithMockUser(username = "mario")
    void corpoNonValidoRestituisce400ConElencoCampi() throws Exception {
        mvc.perform(post("/api/movies/{id}/reviews", filmId).with(csrf())
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "it")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testo\": \"corto\", \"voto\": 9}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errori", hasSize(2)))
                // testi presi da messages.properties (chiavi Max.voto e Size.testo)
                .andExpect(jsonPath("$.errori[?(@.campo == 'voto')].messaggio").value("Il voto va da 1 a 5"))
                .andExpect(jsonPath("$.errori[?(@.campo == 'testo')].messaggio")
                        .value("Il testo deve avere tra 10 e 2000 caratteri"));
    }

    @Test
    @WithMockUser(username = "mario")
    void messaggiDiValidazioneNellaLinguaRichiesta() throws Exception {
        // stessa chiave Max.voto, ma dal file messages_en.properties
        mvc.perform(post("/api/movies/{id}/reviews", filmId).with(csrf())
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testo\": \"Un testo abbastanza lungo\", \"voto\": 9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errori[0].messaggio").value("The rating goes from 1 to 5"));
    }

    @Test
    void inserimentoSenzaLoginRestituisce401() throws Exception {
        mvc.perform(post("/api/movies/{id}/reviews", filmId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testo\": \"Recensione anonima\", \"voto\": 3}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "mario")
    void modificareLaRecensioneDiUnAltroRestituisce403() throws Exception {
        mvc.perform(put("/api/reviews/{id}", recensioneDiLuigiId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testo\": \"La riscrivo io\", \"voto\": 5}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Operazione non consentita"));
        mvc.perform(delete("/api/reviews/{id}", recensioneDiLuigiId).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "luigi")
    void lAutoreModificaEdElimina() throws Exception {
        mvc.perform(put("/api/reviews/{id}", recensioneDiLuigiId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testo\": \"Rivisto: molto meglio\", \"voto\": 4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voto").value(4));
        mvc.perform(delete("/api/reviews/{id}", recensioneDiLuigiId).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(recensioneRepository.count()).isZero();
    }

    @Test
    @WithMockUser(username = "luigi")
    void eliminareUnaRecensioneInesistenteRestituisce404() throws Exception {
        mvc.perform(delete("/api/reviews/{id}", 99999).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- Utente corrente e documentazione ---

    @Test
    void meDaAnonimo() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autenticato").value(false))
                .andExpect(jsonPath("$.username").doesNotExist());
    }

    @Test
    @WithMockUser(username = "mario", roles = "USER")
    void meDaUtenteAutenticato() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(jsonPath("$.autenticato").value(true))
                .andExpect(jsonPath("$.username").value("mario"))
                .andExpect(jsonPath("$.ruolo").value("USER"));
    }

    @Test
    void specificaOpenApiDisponibile() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/movies/{id}/reviews']").exists());
    }
}
