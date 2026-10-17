package it.uniroma3.siw.festival.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.uniroma3.siw.festival.dto.api.RecensioneDto;
import it.uniroma3.siw.festival.dto.form.RecensioneForm;
import it.uniroma3.siw.festival.service.RecensioneService;
import jakarta.validation.Valid;

/**
 * Modifica ed eliminazione di una recensione. Il controllo "solo l'autore" è nel service:
 * se l'utente non è l'autore il service lancia OperazioneNonAutorizzataException → 403.
 */
@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Film e recensioni")
public class ReviewApiController {

    private final RecensioneService recensioneService;

    public ReviewApiController(RecensioneService recensioneService) {
        this.recensioneService = recensioneService;
    }

    @Operation(summary = "Una recensione")
    @GetMapping("/{id}")
    public ResponseEntity<RecensioneDto> dettaglio(@PathVariable Long id) {
        return ResponseEntity.ok(RecensioneDto.da(recensioneService.trova(id)));
    }

    @Operation(summary = "Modifica la propria recensione (403 se non sei l'autore)")
    @PutMapping("/{id}")
    public ResponseEntity<RecensioneDto> modifica(@PathVariable Long id, @Valid @RequestBody RecensioneForm form,
            Authentication authentication) {
        return ResponseEntity.ok(RecensioneDto.da(recensioneService.modifica(id, authentication.getName(), form)));
    }

    @Operation(summary = "Elimina la propria recensione (403 se non sei l'autore)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> elimina(@PathVariable Long id, Authentication authentication) {
        recensioneService.elimina(id, authentication.getName());
        return ResponseEntity.noContent().build(); // 204: eseguito, nessun contenuto da restituire
    }
}
