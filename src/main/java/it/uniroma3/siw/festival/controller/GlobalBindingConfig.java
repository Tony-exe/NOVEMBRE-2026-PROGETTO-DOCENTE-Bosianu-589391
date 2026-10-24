package it.uniroma3.siw.festival.controller;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Regola di binding valida per tutti i controller (lezione 08): ogni stringa che arriva da un form
 * viene ripulita dagli spazi iniziali e finali prima di finire nel form object, così " Venezia "
 * non viene salvato con gli spazi. Con true una stringa vuota diventa null, e quindi @NotBlank /
 * @NotNull la segnalano come campo mancante.
 * Non riguarda i corpi JSON delle API (@RequestBody), che non passano dal data binding.
 */
@ControllerAdvice
public class GlobalBindingConfig {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
