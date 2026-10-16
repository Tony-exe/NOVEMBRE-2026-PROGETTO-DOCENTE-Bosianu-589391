package it.uniroma3.siw.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import it.uniroma3.siw.festival.dto.form.RecensioneForm;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.exception.OperazioneNonAutorizzataException;
import it.uniroma3.siw.festival.exception.RecensioneDuplicataException;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Recensione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.Ruolo;
import it.uniroma3.siw.festival.model.Utente;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.RecensioneRepository;
import it.uniroma3.siw.festival.repository.UtenteRepository;

@ExtendWith(MockitoExtension.class)
class RecensioneServiceTest {

    @Mock
    private RecensioneRepository recensioneRepository;
    @Mock
    private FilmRepository filmRepository;
    @Mock
    private UtenteRepository utenteRepository;

    @InjectMocks
    private RecensioneService service;

    private Film film;
    private Utente mario;
    private Recensione recensioneDiMario;
    private final RecensioneForm form = new RecensioneForm("Un film davvero notevole", 4);

    @BeforeEach
    void setUp() {
        film = new Film("Film", 2020, 100, "Drammatico", "Italia", new Regista());
        film.setId(1L);
        mario = new Utente("mario", "hash", Ruolo.USER);
        mario.setId(5L);
        recensioneDiMario = new Recensione("Testo originale della recensione", 3, LocalDate.now(), film, mario);
        recensioneDiMario.setId(7L);

        lenient().when(filmRepository.findById(1L)).thenReturn(Optional.of(film));
        lenient().when(utenteRepository.findByUsername("mario")).thenReturn(Optional.of(mario));
        lenient().when(recensioneRepository.findByIdWithUtenteAndFilm(7L)).thenReturn(Optional.of(recensioneDiMario));
    }

    @Test
    void secondaRecensioneDelloStessoFilmVieneRifiutata() {
        when(recensioneRepository.existsByFilmIdAndUtenteId(1L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.crea(1L, "mario", form)).isInstanceOf(RecensioneDuplicataException.class);
        verify(recensioneRepository, never()).saveAndFlush(any());
    }

    @Test
    void violazioneDelVincoloUniqueDiventaRecensioneDuplicata() {
        // caso concorrente: il controllo preventivo passa, ma l'INSERT viola il vincolo nel DB
        when(recensioneRepository.existsByFilmIdAndUtenteId(1L, 5L)).thenReturn(false);
        when(recensioneRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.crea(1L, "mario", form)).isInstanceOf(RecensioneDuplicataException.class);
    }

    @Test
    void creazioneValida() {
        when(recensioneRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Recensione r = service.crea(1L, "mario", form);

        assertThat(r.getUtente()).isSameAs(mario);
        assertThat(r.getVoto()).isEqualTo(4);
        assertThat(r.getData()).isEqualTo(LocalDate.now());
    }

    @Test
    void filmInesistente() {
        assertThatThrownBy(() -> service.crea(99L, "mario", form)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void autorePuoModificareLaPropriaRecensione() {
        Recensione r = service.modifica(7L, "mario", form);

        assertThat(r.getTesto()).isEqualTo("Un film davvero notevole");
        assertThat(r.getVoto()).isEqualTo(4);
    }

    @Test
    void unAltroUtenteNonPuoModificareNeEliminare() {
        assertThatThrownBy(() -> service.modifica(7L, "luigi", form))
                .isInstanceOf(OperazioneNonAutorizzataException.class);
        assertThatThrownBy(() -> service.elimina(7L, "luigi"))
                .isInstanceOf(OperazioneNonAutorizzataException.class);

        assertThat(recensioneDiMario.getTesto()).isEqualTo("Testo originale della recensione");
        verify(recensioneRepository, never()).delete(any());
    }

    @Test
    void recensioneInesistente() {
        assertThatThrownBy(() -> service.elimina(99L, "mario")).isInstanceOf(EntityNotFoundException.class);
    }
}
