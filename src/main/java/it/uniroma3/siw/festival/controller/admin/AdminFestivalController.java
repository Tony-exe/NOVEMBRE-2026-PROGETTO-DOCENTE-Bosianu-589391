package it.uniroma3.siw.festival.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw.festival.dto.form.FestivalForm;
import it.uniroma3.siw.festival.exception.RegolaBusinessException;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.service.FestivalService;
import it.uniroma3.siw.festival.service.FilmService;
import jakarta.validation.Valid;

/**
 * Gestione dei festival e dei film partecipanti.
 * Schema di ogni form: GET mostra il form, POST valida (@Valid + BindingResult); se ci sono errori
 * si rimostra il form con i messaggi accanto ai campi, altrimenti si chiama il service e si fa
 * redirect con un messaggio flash (Post/Redirect/Get).
 */
@Controller
@RequestMapping("/admin/festivals")
public class AdminFestivalController {

    private final FestivalService festivalService;
    private final FilmService filmService;

    public AdminFestivalController(FestivalService festivalService, FilmService filmService) {
        this.festivalService = festivalService;
        this.filmService = filmService;
    }

    @GetMapping
    public String elenco(Model model) {
        model.addAttribute("festivals", festivalService.elenco());
        return "admin/festivals/list";
    }

    @GetMapping("/new")
    public String nuovo(Model model) {
        model.addAttribute("form", new FestivalForm());
        return "admin/festivals/form";
    }

    @PostMapping
    public String crea(@Valid @ModelAttribute("form") FestivalForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/festivals/form";
        }
        try {
            Festival festival = festivalService.crea(form);
            redirectAttributes.addFlashAttribute("successo", "Festival \"" + festival.getNome() + "\" creato");
            return "redirect:/admin/festivals";
        } catch (RegolaBusinessException e) {
            bindingResult.reject("regola", e.getMessage());
            return "admin/festivals/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String modificaForm(@PathVariable Long id, Model model) {
        model.addAttribute("festivalId", id);
        model.addAttribute("form", FestivalForm.da(festivalService.trova(id)));
        return "admin/festivals/form";
    }

    @PostMapping("/{id}")
    public String modifica(@PathVariable Long id, @Valid @ModelAttribute("form") FestivalForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("festivalId", id);
        if (bindingResult.hasErrors()) {
            return "admin/festivals/form";
        }
        try {
            festivalService.modifica(id, form);
            redirectAttributes.addFlashAttribute("successo", "Festival aggiornato");
            return "redirect:/admin/festivals";
        } catch (RegolaBusinessException e) {
            bindingResult.reject("regola", e.getMessage());
            return "admin/festivals/form";
        }
    }

    // --- Film partecipanti (la relazione molti-a-molti) ---

    @GetMapping("/{id}/films")
    public String films(@PathVariable Long id, Model model) {
        model.addAttribute("festival", festivalService.trova(id));
        model.addAttribute("films", filmService.filmDelFestival(id));
        model.addAttribute("associabili", filmService.filmAssociabili(id));
        return "admin/festivals/films";
    }

    @PostMapping("/{id}/films")
    public String aggiungiFilm(@PathVariable Long id, @RequestParam Long filmId,
            RedirectAttributes redirectAttributes) {
        try {
            festivalService.aggiungiFilm(id, filmId);
            redirectAttributes.addFlashAttribute("successo", "Film aggiunto al festival");
        } catch (RegolaBusinessException e) {
            redirectAttributes.addFlashAttribute("errore", e.getMessage());
        }
        return "redirect:/admin/festivals/" + id + "/films";
    }

    @PostMapping("/{id}/films/{filmId}/remove")
    public String rimuoviFilm(@PathVariable Long id, @PathVariable Long filmId,
            RedirectAttributes redirectAttributes) {
        try {
            int annullate = festivalService.rimuoviFilm(id, filmId);
            redirectAttributes.addFlashAttribute("successo",
                    "Film rimosso dal festival; proiezioni annullate: " + annullate);
        } catch (RegolaBusinessException e) {
            redirectAttributes.addFlashAttribute("errore", e.getMessage());
        }
        return "redirect:/admin/festivals/" + id + "/films";
    }
}
