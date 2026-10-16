package it.uniroma3.siw.festival.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import it.uniroma3.siw.festival.service.FestivalService;
import it.uniroma3.siw.festival.service.FilmService;

/**
 * Pagine pubbliche dei festival. Il programma delle proiezioni e il dettaglio film sono pagine React
 * (/app/...): da qui ci si arriva con normali link.
 */
@Controller
public class FestivalController {

    private final FestivalService festivalService;
    private final FilmService filmService;

    public FestivalController(FestivalService festivalService, FilmService filmService) {
        this.festivalService = festivalService;
        this.filmService = filmService;
    }

    @GetMapping("/festivals")
    public String elenco(Model model) {
        model.addAttribute("festivals", festivalService.elenco());
        return "festivals/list";
    }

    @GetMapping("/festivals/{id}")
    public String dettaglio(@PathVariable Long id, Model model) {
        model.addAttribute("festival", festivalService.trova(id)); // 404 se non esiste (MvcExceptionHandler)
        return "festivals/detail";
    }

    @GetMapping("/festivals/{id}/films")
    public String films(@PathVariable Long id, Model model) {
        model.addAttribute("festival", festivalService.trova(id));
        // film con il regista già caricato: la vista lo mostra senza query aggiuntive (no N+1)
        model.addAttribute("films", filmService.filmDelFestival(id));
        return "festivals/films";
    }
}
