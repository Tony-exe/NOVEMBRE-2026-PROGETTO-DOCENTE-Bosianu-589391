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

import it.uniroma3.siw.festival.dto.form.SalaForm;
import it.uniroma3.siw.festival.service.SalaService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/sale")
public class AdminSalaController {

    private final SalaService salaService;

    public AdminSalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @GetMapping
    public String elenco(Model model) {
        model.addAttribute("sale", salaService.elenco());
        return "admin/sale/list";
    }

    @GetMapping("/new")
    public String nuova(Model model) {
        model.addAttribute("form", new SalaForm());
        return "admin/sale/form";
    }

    @PostMapping
    public String crea(@Valid @ModelAttribute("form") SalaForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/sale/form";
        }
        salaService.crea(form);
        redirectAttributes.addFlashAttribute("successo", "Sala inserita");
        return "redirect:/admin/sale";
    }

    @GetMapping("/{id}/edit")
    public String modificaForm(@PathVariable Long id, Model model) {
        model.addAttribute("salaId", id);
        model.addAttribute("form", SalaForm.da(salaService.trova(id)));
        return "admin/sale/form";
    }

    @PostMapping("/{id}")
    public String modifica(@PathVariable Long id, @Valid @ModelAttribute("form") SalaForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("salaId", id);
            return "admin/sale/form";
        }
        salaService.modifica(id, form);
        redirectAttributes.addFlashAttribute("successo", "Sala aggiornata");
        return "redirect:/admin/sale";
    }
}
