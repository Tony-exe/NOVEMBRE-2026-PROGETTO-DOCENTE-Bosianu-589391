package it.uniroma3.siw.festival.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import it.uniroma3.siw.festival.model.Ruolo;
import it.uniroma3.siw.festival.model.Utente;
import it.uniroma3.siw.festival.repository.UtenteRepository;

/**
 * Test della catena di filtri di Spring Security tramite MockMvc: le richieste attraversano i filtri
 * veri (autenticazione, CSRF, autorizzazione sugli URL) senza avviare un server HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        utenteRepository.save(new Utente("admin", passwordEncoder.encode("admin"), Ruolo.ADMIN));
        utenteRepository.save(new Utente("user1", passwordEncoder.encode("user1"), Ruolo.USER));
    }

    @AfterEach
    void tearDown() {
        jdbc.execute("TRUNCATE utente RESTART IDENTITY CASCADE");
    }

    // --- Autorizzazione sulle pagine ---

    @Test
    void anonimoSuAreaAdminVieneMandatoAlLogin() throws Exception {
        mvc.perform(get("/admin/festivals"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void utenteSenzaRuoloAdminRiceve403() throws Exception {
        mvc.perform(get("/admin/festivals")).andExpect(status().isForbidden());
    }

    @Test
    void homeELoginSonoPubbliche() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    // --- API: 401 e 403 in JSON, mai redirect al login ---

    @Test
    void anonimoSuApiDiScritturaRiceve401Json() throws Exception {
        mvc.perform(post("/api/movies/1/reviews").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "USER")
    void utenteSuApiRiservataAllAdminRiceve403Json() throws Exception {
        mvc.perform(post("/api/festivals").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "USER")
    void scritturaSenzaTokenCsrfVieneRifiutata() throws Exception {
        mvc.perform(post("/api/movies/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Token CSRF mancante o non valido"));
    }

    // --- Login con form ---

    @Test
    void loginConCredenzialiCorrette() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("admin"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"));
    }

    @Test
    void loginConPasswordSbagliata() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("sbagliata"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    // --- Registrazione ---

    @Test
    void registrazioneValidaCreaUnUtenteUser() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "nuovo").param("password", "segreta"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("messaggio"));

        Utente nuovo = utenteRepository.findByUsername("nuovo").orElseThrow();
        assertThat(nuovo.getRuolo()).isEqualTo(Ruolo.USER);
        assertThat(passwordEncoder.matches("segreta", nuovo.getPassword())).isTrue();
    }

    @Test
    void registrazioneConUsernameEsistenteMostraErroreSulCampo() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "user1").param("password", "segreta"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "username"));
    }

    @Test
    void registrazioneConPasswordTroppoCorta() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "nuovo").param("password", "x"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "password"));
        assertThat(utenteRepository.existsByUsername("nuovo")).isFalse();
    }
}
