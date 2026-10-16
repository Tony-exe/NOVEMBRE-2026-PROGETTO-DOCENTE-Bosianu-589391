package it.uniroma3.siw.festival.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw.festival.dto.form.ProiezioneForm;
import it.uniroma3.siw.festival.exception.ConflittoProgrammazioneException;
import it.uniroma3.siw.festival.exception.RegolaBusinessException;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.service.FestivalService;
import it.uniroma3.siw.festival.service.FilmService;
import it.uniroma3.siw.festival.service.ProiezioneService;
import it.uniroma3.siw.festival.service.SalaService;
import jakarta.validation.Valid;

/**
 * Programmazione, modifica, annullamento delle proiezioni di un festival.
 * Il controller non verifica nulla sul calendario: periodo del festival, sovrapposizioni ecc.
 * sono regole del service; qui si traducono le sue eccezioni in messaggi nel form.
 */
@Controller
public class AdminProiezioneController {

    private final ProiezioneService proiezioneService;
    private final FestivalService festivalService;
    private final FilmService filmService;
    private final SalaService salaService;

    public AdminProiezioneController(ProiezioneService proiezioneService, FestivalService festivalService,
            FilmService filmService, SalaService salaService) {
        this.proiezioneService = proiezioneService;
        this.festivalService = festivalService;
        this.filmService = filmService;
        this.salaService = salaService;
    }

    @GetMapping("/admin/festivals/{festivalId}/proiezioni")
    public String programma(@PathVariable Long festivalId, Model model) {
        model.addAttribute("form", new ProiezioneForm());
        return mostraProgramma(festivalId, model);
    }

    @PostMapping("/admin/festivals/{festivalId}/proiezioni")
    public String programmaProiezione(@PathVariable Long festivalId, @Valid @ModelAttribute("form") ProiezioneForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return mostraProgramma(festivalId, model);
        }
        try {
            proiezioneService.programmaProiezione(festivalId, form.getFilmId(), form.getSalaId(), form.getData(),
                    form.getOra());
        } catch (RegolaBusinessException | ConflittoProgrammazioneException e) {
            // la transazione del service è già stata annullata: mostro il motivo sopra il form
            bindingResult.reject("regola", e.getMessage());
            return mostraProgramma(festivalId, model);
        }
        redirectAttributes.addFlashAttribute("successo", "Proiezione programmata");
        return "redirect:/admin/festivals/" + festivalId + "/proiezioni";
    }

    @GetMapping("/admin/proiezioni/{id}/edit")
    public String modificaForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Proiezione proiezione = proiezioneService.trova(id);
        if (!proiezione.isModificabile()) {
            redirectAttributes.addFlashAttribute("errore", "Una proiezione " + proiezione.getStato()
                    + " non è modificabile");
            return "redirect:/admin/festivals/" + proiezione.getFestival().getId() + "/proiezioni";
        }
        model.addAttribute("form", ProiezioneForm.da(proiezione));
        return mostraModifica(proiezione, model);
    }

    @PostMapping("/admin/proiezioni/{id}")
    public String modifica(@PathVariable Long id, @Valid @ModelAttribute("form") ProiezioneForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return mostraModifica(proiezioneService.trova(id), model);
        }
        Proiezione proiezione;
        try {
            proiezione = proiezioneService.modificaProiezione(id, form.getSalaId(), form.getData(), form.getOra());
        } catch (RegolaBusinessException | ConflittoProgrammazioneException e) {
            bindingResult.reject("regola", e.getMessage());
            return mostraModifica(proiezioneService.trova(id), model);
        }
        redirectAttributes.addFlashAttribute("successo", "Proiezione aggiornata");
        return "redirect:/admin/festivals/" + proiezione.getFestival().getId() + "/proiezioni";
    }

    @PostMapping("/admin/proiezioni/{id}/annulla")
    public String annulla(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long festivalId = proiezioneService.trova(id).getFestival().getId();
        try {
            proiezioneService.annulla(id);
            redirectAttributes.addFlashAttribute("successo", "Proiezione annullata");
        } catch (RegolaBusinessException e) {
            redirectAttributes.addFlashAttribute("errore", e.getMessage());
        }
        return "redirect:/admin/festivals/" + festivalId + "/proiezioni";
    }

    @PostMapping("/admin/proiezioni/{id}/completa")
    public String completa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long festivalId = proiezioneService.trova(id).getFestival().getId();
        try {
            proiezioneService.segnaCompletata(id);
            redirectAttributes.addFlashAttribute("successo", "Proiezione segnata come completata");
        } catch (RegolaBusinessException e) {
            redirectAttributes.addFlashAttribute("errore", e.getMessage());
        }
        return "redirect:/admin/festivals/" + festivalId + "/proiezioni";
    }

    // Programma (con film e sala già caricati) + dati per le select del form di programmazione
    private String mostraProgramma(Long festivalId, Model model) {
        model.addAttribute("festival", festivalService.trova(festivalId));
        model.addAttribute("proiezioni", proiezioneService.programma(festivalId, null));
        model.addAttribute("films", filmService.filmDelFestival(festivalId));
        model.addAttribute("sale", salaService.elenco());
        return "admin/proiezioni/list";
    }

    private String mostraModifica(Proiezione proiezione, Model model) {
        model.addAttribute("proiezione", proiezione); // caricata con festival, film e sala
        model.addAttribute("sale", salaService.elenco());
        return "admin/proiezioni/form";
    }
}
