package it.uniroma3.siw.festival.controller.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.uniroma3.siw.festival.dto.api.FestivalDto;
import it.uniroma3.siw.festival.dto.api.FilmDto;
import it.uniroma3.siw.festival.dto.api.ProiezioneDto;
import it.uniroma3.siw.festival.service.FestivalService;
import it.uniroma3.siw.festival.service.FilmService;
import it.uniroma3.siw.festival.service.ProiezioneService;

/**
 * API pubbliche sui festival. Il controller converte le entità (già caricate dal service) in DTO;
 * gli errori (es. 404) sono tradotti in ProblemDetail da ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/festivals")
@Tag(name = "Festival")
public class FestivalApiController {

    private final FestivalService festivalService;
    private final FilmService filmService;
    private final ProiezioneService proiezioneService;

    public FestivalApiController(FestivalService festivalService, FilmService filmService,
            ProiezioneService proiezioneService) {
        this.festivalService = festivalService;
        this.filmService = filmService;
        this.proiezioneService = proiezioneService;
    }

    @Operation(summary = "Elenco dei festival")
    @GetMapping
    public ResponseEntity<List<FestivalDto>> elenco() {
        return ResponseEntity.ok(festivalService.elenco().stream().map(FestivalDto::da).toList());
    }

    @Operation(summary = "Dettaglio di un festival")
    @GetMapping("/{id}")
    public ResponseEntity<FestivalDto> dettaglio(@PathVariable Long id) {
        return ResponseEntity.ok(FestivalDto.da(festivalService.trova(id)));
    }

    @Operation(summary = "Film partecipanti a un festival")
    @GetMapping("/{id}/movies")
    public ResponseEntity<List<FilmDto>> films(@PathVariable Long id) {
        return ResponseEntity.ok(filmService.filmDelFestival(id).stream().map(FilmDto::da).toList());
    }

    @Operation(summary = "Programma delle proiezioni, ordinato per data e ora, con filtro opzionale per giorno")
    @GetMapping("/{id}/screenings")
    public ResponseEntity<List<ProiezioneDto>> programma(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(proiezioneService.programma(id, date).stream()
                .map(ProiezioneDto::perProgramma)
                .toList());
    }
}
