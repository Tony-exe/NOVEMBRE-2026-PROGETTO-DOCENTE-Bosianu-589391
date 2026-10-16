package it.uniroma3.siw.festival.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Pannello amministrativo. L'accesso a /admin/** è riservato ad ADMIN dalla SecurityFilterChain. */
@Controller
public class AdminController {

    @GetMapping("/admin")
    public String pannello() {
        return "admin/dashboard";
    }
}
