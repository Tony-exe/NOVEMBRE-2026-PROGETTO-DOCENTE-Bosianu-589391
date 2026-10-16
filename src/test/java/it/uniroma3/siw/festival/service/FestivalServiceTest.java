package it.uniroma3.siw.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.uniroma3.siw.festival.dto.form.FestivalForm;
import it.uniroma3.siw.festival.exception.RegolaBusinessException;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.StatoProiezione;
import it.uniroma3.siw.festival.repository.FestivalRepository;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;

@ExtendWith(MockitoExtension.class)
class FestivalServiceTest {

    @Mock
    private FestivalRepository festivalRepository;
    @Mock
    private FilmRepository filmRepository;
    @Mock
    private ProiezioneRepository proiezioneRepository;

    @InjectMocks
    private FestivalService service;

    private Festival festival;
    private Film film;

    @BeforeEach
    void setUp() {
        festival = new Festival("Festival", 2030, "Roma", LocalDate.of(2030, 5, 1), LocalDate.of(2030, 5, 10), null);
        festival.setId(1L);
        film = new Film("Film", 2020, 100, "Drammatico", "Italia", new Regista());
        film.setId(2L);
        lenient().when(festivalRepository.findByIdWithFilms(1L)).thenReturn(Optional.of(festival));
        lenient().when(festivalRepository.findById(1L)).thenReturn(Optional.of(festival));
        lenient().when(filmRepository.findById(2L)).thenReturn(Optional.of(film));
    }

    private FestivalForm form(LocalDate inizio, LocalDate fine) {
        FestivalForm form = FestivalForm.da(festival);
        form.setDataInizio(inizio);
        form.setDataFine(fine);
        return form;
    }

    @Test
    void periodoConFinePrimaDellInizioVieneRifiutato() {
        assertThatThrownBy(() -> service.crea(form(LocalDate.of(2030, 5, 10), LocalDate.of(2030, 5, 1))))
                .isInstanceOf(RegolaBusinessException.class);
        verify(festivalRepository, never()).save(any());
    }

    @Test
    void nonSiRestringeIlPeriodoSeEscludeProiezioniProgrammate() {
        LocalDate inizio = LocalDate.of(2030, 5, 3);
        LocalDate fine = LocalDate.of(2030, 5, 10);
        when(proiezioneRepository.esistonoProiezioniFuoriPeriodo(1L, inizio, fine)).thenReturn(true);

        assertThatThrownBy(() -> service.modifica(1L, form(inizio, fine)))
                .isInstanceOf(RegolaBusinessException.class);
        assertThat(festival.getDataInizio()).isEqualTo(LocalDate.of(2030, 5, 1)); // invariato
    }

    @Test
    void rimuovereUnFilmAnnullaSoloLeProiezioniProgrammate() {
        festival.addFilm(film);
        Proiezione programmata = new Proiezione();
        programmata.setStato(StatoProiezione.SCHEDULED);
        Proiezione svolta = new Proiezione();
        svolta.setStato(StatoProiezione.COMPLETED);
        when(proiezioneRepository.findByFestivalIdAndFilmId(1L, 2L)).thenReturn(List.of(programmata, svolta));

        int annullate = service.rimuoviFilm(1L, 2L);

        assertThat(annullate).isEqualTo(1);
        assertThat(programmata.getStato()).isEqualTo(StatoProiezione.CANCELLED);
        assertThat(svolta.getStato()).isEqualTo(StatoProiezione.COMPLETED);
        assertThat(festival.getFilms()).doesNotContain(film);
        assertThat(film.getFestivals()).doesNotContain(festival);
    }

    @Test
    void nonSiRimuoveUnFilmCheNonPartecipa() {
        assertThatThrownBy(() -> service.rimuoviFilm(1L, 2L)).isInstanceOf(RegolaBusinessException.class);
    }

    @Test
    void nonSiAggiungeDueVolteLoStessoFilm() {
        festival.addFilm(film);
        assertThatThrownBy(() -> service.aggiungiFilm(1L, 2L)).isInstanceOf(RegolaBusinessException.class);
    }
}
