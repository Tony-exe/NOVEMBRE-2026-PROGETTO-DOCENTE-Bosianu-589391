package it.uniroma3.siw.festival.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import it.uniroma3.siw.festival.model.Attore;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.Sala;
import it.uniroma3.siw.festival.model.StatoProiezione;
import it.uniroma3.siw.festival.repository.AttoreRepository;
import it.uniroma3.siw.festival.repository.FestivalRepository;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;
import it.uniroma3.siw.festival.repository.RegistaRepository;
import it.uniroma3.siw.festival.repository.SalaRepository;
import it.uniroma3.siw.festival.support.DatabaseCleaner;

/**
 * Test delle pagine Thymeleaf attraverso tutto lo stack (sicurezza, controller, service, DB, template).
 * Con open-in-view=false, se un template accedesse a un'associazione LAZY non caricata dal service,
 * il rendering fallirebbe con LazyInitializationException: questi test lo intercettano.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PagineMvcTest {

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
    private JdbcTemplate jdbc;

    private Long festivalId;
    private Long filmId;
    private Long registaId;
    private Long attoreId;
    private Long salaId;
    private Long proiezioneId;
    private LocalDate giorno;

    @BeforeEach
    void setUp() {
        LocalDate oggi = LocalDate.now();
        giorno = oggi.plusDays(12);
        Regista regista = registaRepository.save(new Regista("Francis Ford", "Coppola", LocalDate.of(1939, 4, 7), "Statunitense"));
        Attore attore = attoreRepository.save(new Attore("Marlon", "Brando", LocalDate.of(1924, 4, 3), "Statunitense"));
        Film film = new Film("Il Padrino", 1972, 120, "Drammatico", "Stati Uniti", regista);
        film.addInterpretazione(attore, "Don Vito Corleone");
        filmRepository.save(film);
        Festival festival = new Festival("Festa del Cinema", oggi.getYear(), "Roma", oggi.plusDays(10), oggi.plusDays(20),
                "Rassegna di prova");
        festival.addFilm(film);
        festivalRepository.save(festival);
        Sala sala = salaRepository.save(new Sala("Sala Sinopoli", "Viale de Coubertin 30", 1200));

        Proiezione proiezione = new Proiezione();
        festival.addProiezione(proiezione);
        film.addProiezione(proiezione);
        sala.addProiezione(proiezione);
        proiezione.impostaOrario(giorno, LocalTime.of(14, 0));
        proiezioneRepository.save(proiezione);

        festivalId = festival.getId();
        filmId = film.getId();
        registaId = regista.getId();
        attoreId = attore.getId();
        salaId = sala.getId();
        proiezioneId = proiezione.getId();
    }

    @AfterEach
    void tearDown() {
        DatabaseCleaner.svuota(jdbc);
    }

    // --- Pagine pubbliche ---

    @Test
    void paginePubblicheSiRenderizzano() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Festa del Cinema")));
        mvc.perform(get("/festivals")).andExpect(status().isOk());
        mvc.perform(get("/festivals/{id}", festivalId)).andExpect(status().isOk())
                .andExpect(content().string(containsString("/app/festivals/" + festivalId + "/screenings")));
        // il regista viene mostrato: deve essere stato caricato dal service (JOIN FETCH)
        mvc.perform(get("/festivals/{id}/films", festivalId)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Francis Ford Coppola")));
    }

    @Test
    void festivalInesistenteMostraLaPagina404() throws Exception {
        mvc.perform(get("/festivals/{id}", 99999))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("messaggio", "Festival con id 99999 non trovato"));
    }

    @Test
    void laNavbarMostraLaGestioneSoloAllAdmin() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(org.hamcrest.Matchers.not(containsString("/admin/films"))));
    }

    // --- Area admin: tutte le pagine si renderizzano ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void tutteLePagineAdminSiRenderizzano() throws Exception {
        String[] pagine = {
                "/admin", "/admin/festivals", "/admin/festivals/new", "/admin/festivals/" + festivalId + "/edit",
                "/admin/festivals/" + festivalId + "/films", "/admin/festivals/" + festivalId + "/proiezioni",
                "/admin/proiezioni/" + proiezioneId + "/edit",
                "/admin/films", "/admin/films/new", "/admin/films/" + filmId + "/edit", "/admin/films/" + filmId + "/cast",
                "/admin/registi", "/admin/registi/new", "/admin/registi/" + registaId + "/edit",
                "/admin/attori", "/admin/attori/new", "/admin/attori/" + attoreId + "/edit",
                "/admin/sale", "/admin/sale/new", "/admin/sale/" + salaId + "/edit" };
        for (String pagina : pagine) {
            mvc.perform(get(pagina)).andExpect(status().isOk());
        }
        mvc.perform(get("/")).andExpect(content().string(containsString("/admin/films")));
    }

    // --- Form: validazione e regole di business ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void festivalConFinePrimaDellInizioMostraErroreSulCampo() throws Exception {
        mvc.perform(post("/admin/festivals").with(csrf())
                        .param("nome", "Festival sbagliato").param("anno", "2030").param("citta", "Roma")
                        .param("dataInizio", "2030-05-10").param("dataFine", "2030-05-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festivals/form"))
                .andExpect(model().attributeHasFieldErrors("form", "dataFine"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void festivalValidoVieneCreatoConRedirectEMessaggioFlash() throws Exception {
        mvc.perform(post("/admin/festivals").with(csrf())
                        .param("nome", "Torino Film Festival").param("anno", "2030").param("citta", "Torino")
                        .param("dataInizio", "2030-11-20").param("dataFine", "2030-11-28"))
                .andExpect(redirectedUrl("/admin/festivals"))
                .andExpect(flash().attribute("successo", "Festival \"Torino Film Festival\" creato"));
        assertThat(festivalRepository.count()).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void proiezioneSovrappostaRimostraIlFormConIlMotivo() throws Exception {
        mvc.perform(post("/admin/festivals/{id}/proiezioni", festivalId).with(csrf())
                        .param("filmId", filmId.toString()).param("salaId", salaId.toString())
                        .param("data", giorno.toString()).param("ora", "15:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/proiezioni/list"))
                .andExpect(model().attributeHasErrors("form"))
                .andExpect(content().string(containsString("è già occupata")));
        assertThat(proiezioneRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void proiezioneValidaVieneProgrammata() throws Exception {
        mvc.perform(post("/admin/festivals/{id}/proiezioni", festivalId).with(csrf())
                        .param("filmId", filmId.toString()).param("salaId", salaId.toString())
                        .param("data", giorno.toString()).param("ora", "16:30"))
                .andExpect(redirectedUrl("/admin/festivals/" + festivalId + "/proiezioni"));
        assertThat(proiezioneRepository.count()).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rimozioneDelFilmDalFestivalAnnullaLeProiezioni() throws Exception {
        mvc.perform(post("/admin/festivals/{id}/films/{filmId}/remove", festivalId, filmId).with(csrf()))
                .andExpect(redirectedUrl("/admin/festivals/" + festivalId + "/films"))
                .andExpect(flash().attribute("successo", "Film rimosso dal festival; proiezioni annullate: 1"));
        assertThat(proiezioneRepository.findById(proiezioneId).orElseThrow().getStato())
                .isEqualTo(StatoProiezione.CANCELLED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unaProiezioneAnnullataNonSiModifica() throws Exception {
        mvc.perform(post("/admin/proiezioni/{id}/annulla", proiezioneId).with(csrf()))
                .andExpect(flash().attribute("successo", "Proiezione annullata"));

        mvc.perform(get("/admin/proiezioni/{id}/edit", proiezioneId))
                .andExpect(redirectedUrl("/admin/festivals/" + festivalId + "/proiezioni"))
                .andExpect(flash().attribute("errore", "Una proiezione CANCELLED non è modificabile"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void castSenzaPersonaggioMostraErroreSulCampo() throws Exception {
        mvc.perform(post("/admin/films/{id}/cast", filmId).with(csrf())
                        .param("attoreId", attoreId.toString()).param("personaggio", " "))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("castForm", "personaggio"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUtenteNormaleNonEntraNellAreaAdmin() throws Exception {
        mvc.perform(get("/admin/festivals")).andExpect(status().isForbidden());
    }
}
