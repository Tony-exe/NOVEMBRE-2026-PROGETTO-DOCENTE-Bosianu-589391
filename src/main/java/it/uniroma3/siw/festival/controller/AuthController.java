package it.uniroma3.siw.festival.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw.festival.dto.form.RegistrazioneForm;
import it.uniroma3.siw.festival.exception.UsernameGiaUsatoException;
import it.uniroma3.siw.festival.service.UtenteService;
import jakarta.validation.Valid;

/**
 * Login e registrazione. Il POST /login NON è qui: lo gestisce il filtro di Spring Security
 * (UsernamePasswordAuthenticationFilter); il controller mostra solo la pagina del form.
 */
@Controller
public class AuthController {

    private final UtenteService utenteService;

    public AuthController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registrazioneForm(Model model) {
        model.addAttribute("form", new RegistrazioneForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registra(@Valid @ModelAttribute("form") RegistrazioneForm form, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            utenteService.registra(form);
        } catch (UsernameGiaUsatoException e) {
            // errore di business mostrato accanto al campo, come un errore di validazione
            bindingResult.rejectValue("username", "duplicato", e.getMessage());
            return "auth/register";
        }
        // Post/Redirect/Get: dopo un POST riuscito si fa redirect, così ricaricare la pagina non re-invia il form
        redirectAttributes.addFlashAttribute("messaggio", "Registrazione completata: ora puoi accedere");
        return "redirect:/login";
    }
}
