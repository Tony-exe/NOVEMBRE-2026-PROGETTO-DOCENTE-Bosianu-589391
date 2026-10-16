package it.uniroma3.siw.festival.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw.festival.dto.form.FilmForm;
import it.uniroma3.siw.festival.dto.form.InterpretazioneForm;
import it.uniroma3.siw.festival.exception.RegolaBusinessException;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.service.AttoreService;
import it.uniroma3.siw.festival.service.FilmService;
import it.uniroma3.siw.festival.service.RegistaService;
import jakarta.validation.Valid;

/** Inserimento e modifica dei film, con regista e cast. */
@Controller
@RequestMapping("/admin/films")
public class AdminFilmController {

    private final FilmService filmService;
    private final RegistaService registaService;
    private final AttoreService attoreService;

    public AdminFilmController(FilmService filmService, RegistaService registaService,
            AttoreService attoreService) {
        this.filmService = filmService;
        this.registaService = registaService;
        this.attoreService = attoreService;
    }

    @GetMapping
    public String elenco(Model model) {
        model.addAttribute("films", filmService.elenco());
        return "admin/films/list";
    }

    @GetMapping("/new")
    public String nuovo(Model model) {
        model.addAttribute("form", new FilmForm());
        return mostraForm(null, model);
    }

    @PostMapping
    public String crea(@Valid @ModelAttribute("form") FilmForm form, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return mostraForm(null, model);
        }
        Film film = filmService.crea(form);
        redirectAttributes.addFlashAttribute("successo", "Film \"" + film.getTitolo() + "\" creato: ora puoi inserire il cast");
        return "redirect:/admin/films/" + film.getId() + "/cast";
    }

    @GetMapping("/{id}/edit")
    public String modificaForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", FilmForm.da(filmService.trovaConCast(id)));
        return mostraForm(id, model);
    }

    @PostMapping("/{id}")
    public String modifica(@PathVariable Long id, @Valid @ModelAttribute("form") FilmForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return mostraForm(id, model);
        }
        try {
            filmService.modifica(id, form);
        } catch (RegolaBusinessException e) {
            bindingResult.reject("regola", e.getMessage());
            return mostraForm(id, model);
        }
        redirectAttributes.addFlashAttribute("successo", "Film aggiornato");
        return "redirect:/admin/films";
    }

    // Il form ha bisogno anche dell'elenco dei registi per la select
    private String mostraForm(Long filmId, Model model) {
        model.addAttribute("filmId", filmId);
        model.addAttribute("registi", registaService.elenco());
        return "admin/films/form";
    }

    // --- Cast ---

    @GetMapping("/{id}/cast")
    public String cast(@PathVariable Long id, Model model) {
        model.addAttribute("castForm", new InterpretazioneForm());
        return mostraCast(id, model);
    }

    @PostMapping("/{id}/cast")
    public String aggiungiInterpretazione(@PathVariable Long id,
            @Valid @ModelAttribute("castForm") InterpretazioneForm form, BindingResult bindingResult, Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return mostraCast(id, model);
        }
        try {
            filmService.aggiungiInterpretazione(id, form.getAttoreId(), form.getPersonaggio());
        } catch (RegolaBusinessException e) {
            bindingResult.reject("regola", e.getMessage());
            return mostraCast(id, model);
        }
        redirectAttributes.addFlashAttribute("successo", "Interpretazione aggiunta");
        return "redirect:/admin/films/" + id + "/cast";
    }

    @PostMapping("/{id}/cast/{interpretazioneId}/remove")
    public String rimuoviInterpretazione(@PathVariable Long id, @PathVariable Long interpretazioneId,
            RedirectAttributes redirectAttributes) {
        filmService.rimuoviInterpretazione(id, interpretazioneId);
        redirectAttributes.addFlashAttribute("successo", "Interpretazione rimossa");
        return "redirect:/admin/films/" + id + "/cast";
    }

    private String mostraCast(Long filmId, Model model) {
        model.addAttribute("film", filmService.trovaConCast(filmId)); // regista e cast già caricati
        model.addAttribute("attori", attoreService.elenco());
        return "admin/films/cast";
    }
}
