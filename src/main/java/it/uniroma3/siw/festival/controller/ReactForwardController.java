package it.uniroma3.siw.festival.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Le pagine React sono gestite dal router nel browser: sul server esiste un solo file, /app/index.html.
 * Se l'utente apre direttamente (o ricarica) /app/movies/3, Spring non ha un file con quel nome:
 * questo controller fa un forward interno a index.html, poi React legge l'URL e mostra la pagina giusta.
 * Elenco esplicitamente le rotte React, così i file statici (/app/assets/...) restano serviti normalmente.
 */
@Controller
public class ReactForwardController {

    @GetMapping({ "/app", "/app/", "/app/movies/{id}", "/app/festivals/{id}/screenings" })
    public String paginaReact() {
        return "forward:/app/index.html";
    }
}
