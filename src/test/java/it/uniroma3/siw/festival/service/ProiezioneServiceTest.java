package it.uniroma3.siw.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.uniroma3.siw.festival.exception.ConflittoProgrammazioneException;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.exception.RegolaBusinessException;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.Sala;
import it.uniroma3.siw.festival.model.StatoProiezione;
import it.uniroma3.siw.festival.repository.FestivalRepository;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;
import it.uniroma3.siw.festival.repository.SalaRepository;

/**
 * Unit test: i repository sono sostituiti da mock, quindi si verifica solo la logica del service
 * (nessun database, nessuna transazione reale).
 */
@ExtendWith(MockitoExtension.class)
class ProiezioneServiceTest {

    private static final Long FESTIVAL_ID = 1L;
    private static final Long FILM_ID = 2L;
    private static final Long SALA_ID = 3L;

    @Mock
    private ProiezioneRepository proiezioneRepository;
    @Mock
    private FestivalRepository festivalRepository;
    @Mock
    private FilmRepository filmRepository;
    @Mock
    private SalaRepository salaRepository;

    @InjectMocks
    private ProiezioneService service;

    private Festival festival;
    private Film film;
    private Sala sala;
    private LocalDate giorno;

    @BeforeEach
    void setUp() {
        LocalDate oggi = LocalDate.now();
        festival = new Festival("Festival", oggi.getYear(), "Roma", oggi.plusDays(10), oggi.plusDays(20), null);
        festival.setId(FESTIVAL_ID);
        film = new Film("Film", 2020, 120, "Drammatico", "Italia", new Regista());
        film.setId(FILM_ID);
        sala = new Sala("Sala 1", "Via Roma 1", 100);
        sala.setId(SALA_ID);
        giorno = oggi.plusDays(12);

        // lenient: non tutti i test arrivano a usare tutti gli stub (alcuni falliscono prima)
        lenient().when(festivalRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(festival));
        lenient().when(filmRepository.findById(FILM_ID)).thenReturn(Optional.of(film));
        lenient().when(festivalRepository.partecipaAlFestival(FESTIVAL_ID, FILM_ID)).thenReturn(true);
        lenient().when(salaRepository.findByIdForUpdate(SALA_ID)).thenReturn(Optional.of(sala));
        lenient().when(proiezioneRepository.save(any(Proiezione.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void programmazioneValidaCreaLaProiezioneEAggiornaLeAssociazioni() {
        Proiezione p = service.programmaProiezione(FESTIVAL_ID, FILM_ID, SALA_ID, giorno, LocalTime.of(14, 0));

        assertThat(p.getOraFine()).isEqualTo(LocalTime.of(16, 0));
        assertThat(p.getStato()).isEqualTo(StatoProiezione.SCHEDULED);
        assertThat(p.getFestival()).isSameAs(festival);
        assertThat(festival.getProiezioni()).contains(p);
        assertThat(film.getProiezioni()).contains(p);
        assertThat(sala.getProiezioni()).contains(p);
        verify(salaRepository).findByIdForUpdate(SALA_ID); // il lock sulla sala viene preso
        verify(proiezioneRepository).save(p);
    }

    @Test
    void filmNonAssociatoAlFestival() {
        when(festivalRepository.partecipaAlFestival(FESTIVAL_ID, FILM_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.programmaProiezione(FESTIVAL_ID, FILM_ID, SALA_ID, giorno, LocalTime.NOON))
                .isInstanceOf(RegolaBusinessException.class)
                .hasMessageContaining("non partecipa");
        verify(proiezioneRepository, never()).save(any());
    }

    @Test
    void dataFuoriDalPeriodoDelFestival() {
        LocalDate dopoLaFine = festival.getDataFine().plusDays(1);

        assertThatThrownBy(() -> service.programmaProiezione(FESTIVAL_ID, FILM_ID, SALA_ID, dopoLaFine, LocalTime.NOON))
                .isInstanceOf(RegolaBusinessException.class)
                .hasMessageContaining("compresa tra");
    }

    @Test
    void nonSiProgrammaNelPassato() {
        // festival in corso: ieri è dentro il periodo ma è nel passato
        festival.setDataInizio(LocalDate.now().minusDays(3));
        LocalDate ieri = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> service.programmaProiezione(FESTIVAL_ID, FILM_ID, SALA_ID, ieri, LocalTime.NOON))
                .isInstanceOf(RegolaBusinessException.class)
                .hasMessageContaining("passato");
    }

    @Test
    void proiezioneOltreLaMezzanotte() {
        assertThatThrownBy(() -> service.programmaProiezione(FESTIVAL_ID, FILM_ID, SALA_ID, giorno, LocalTime.of(23, 0)))
                .isInstanceOf(RegolaBusinessException.class)
                .hasMessageContaining("mezzanotte");
    }

    @Test
    void salaOccupataGeneraConflitto() {
        when(proiezioneRepository.esisteSovrapposizione(SALA_ID, giorno, LocalTime.of(14, 0), LocalTime.of(16, 0), null))
                .thenReturn(true);

        assertThatThrownBy(() -> service.programmaProiezione(FESTIVAL_ID, FILM_ID, SALA_ID, giorno, LocalTime.of(14, 0)))
                .isInstanceOf(ConflittoProgrammazioneException.class);
        verify(proiezioneRepository, never()).save(any());
    }

    @Test
    void salaInesistente() {
        when(salaRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.programmaProiezione(FESTIVAL_ID, FILM_ID, 99L, giorno, LocalTime.NOON))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void proiezioneAnnullataNonEModificabile() {
        Proiezione annullata = new Proiezione();
        annullata.setId(10L);
        annullata.setFestival(festival);
        annullata.setFilm(film);
        annullata.setStato(StatoProiezione.CANCELLED);
        when(proiezioneRepository.findByIdWithDettagli(10L)).thenReturn(Optional.of(annullata));

        assertThatThrownBy(() -> service.modificaProiezione(10L, SALA_ID, giorno, LocalTime.NOON))
                .isInstanceOf(RegolaBusinessException.class)
                .hasMessageContaining("non è modificabile");
        assertThatThrownBy(() -> service.annulla(10L)).isInstanceOf(RegolaBusinessException.class);
    }

    @Test
    void inModificaLaVerificaEscludeLaProiezioneStessa() {
        Proiezione esistente = new Proiezione();
        esistente.setId(10L);
        festival.addProiezione(esistente);
        film.addProiezione(esistente);
        sala.addProiezione(esistente);
        esistente.impostaOrario(giorno, LocalTime.of(14, 0));
        when(proiezioneRepository.findByIdWithDettagli(10L)).thenReturn(Optional.of(esistente));

        service.modificaProiezione(10L, SALA_ID, giorno, LocalTime.of(15, 0));

        verify(proiezioneRepository).esisteSovrapposizione(SALA_ID, giorno, LocalTime.of(15, 0), LocalTime.of(17, 0), 10L);
        assertThat(esistente.getOraFine()).isEqualTo(LocalTime.of(17, 0));
    }
}
