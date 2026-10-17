package it.uniroma3.siw.festival.controller.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.uniroma3.siw.festival.dto.api.FilmDettaglioDto;
import it.uniroma3.siw.festival.dto.api.FilmDto;
import it.uniroma3.siw.festival.dto.api.PaginaDto;
import it.uniroma3.siw.festival.dto.api.RecensioneDto;
import it.uniroma3.siw.festival.dto.api.RecensioniFilmDto;
import it.uniroma3.siw.festival.dto.form.RecensioneForm;
import it.uniroma3.siw.festival.model.Recensione;
import it.uniroma3.siw.festival.service.FilmService;
import it.uniroma3.siw.festival.service.RecensioneService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/movies")
@Tag(name = "Film e recensioni")
public class MovieApiController {

    private final FilmService filmService;
    private final RecensioneService recensioneService;

    public MovieApiController(FilmService filmService, RecensioneService recensioneService) {
        this.filmService = filmService;
        this.recensioneService = recensioneService;
    }

    @Operation(summary = "Ricerca film per titolo, genere e regista (filtri opzionali), paginata")
    @GetMapping
    public ResponseEntity<PaginaDto<FilmDto>> cerca(@RequestParam(required = false) String title,
            @RequestParam(required = false) String genre, @RequestParam(required = false) String director,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(PaginaDto.da(filmService.cerca(title, genre, director, page, size), FilmDto::da));
    }

    @Operation(summary = "Dettaglio di un film: regista, cast, festival, proiezioni e media dei voti")
    @GetMapping("/{id}")
    public ResponseEntity<FilmDettaglioDto> dettaglio(@PathVariable Long id) {
        return ResponseEntity.ok(FilmDettaglioDto.da(filmService.dettaglio(id)));
    }

    @Operation(summary = "Recensioni di un film con la media dei voti")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<RecensioniFilmDto> recensioni(@PathVariable Long id) {
        List<RecensioneDto> recensioni = recensioneService.recensioniDelFilm(id).stream()
                .map(RecensioneDto::da)
                .toList();
        return ResponseEntity.ok(new RecensioniFilmDto(id, recensioneService.mediaVoti(id), recensioni.size(),
                recensioni));
    }

    /*
     * 201 Created con header Location che punta alla nuova risorsa.
     * L'autore è l'utente autenticato (Authentication iniettata da Spring Security), mai un campo del JSON.
     * @Valid sul corpo: se non è valido → MethodArgumentNotValidException → 400 con l'elenco dei campi.
     */
    @Operation(summary = "Inserisce la recensione dell'utente autenticato (409 se ha già recensito il film)")
    @PostMapping("/{id}/reviews")
    public ResponseEntity<RecensioneDto> crea(@PathVariable Long id, @Valid @RequestBody RecensioneForm form,
            Authentication authentication) {
        Recensione recensione = recensioneService.crea(id, authentication.getName(), form);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/reviews/{id}")
                .buildAndExpand(recensione.getId())
                .toUri();
        return ResponseEntity.created(location).body(RecensioneDto.da(recensione));
    }
}
