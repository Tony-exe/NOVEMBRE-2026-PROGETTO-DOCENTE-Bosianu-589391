package it.uniroma3.siw.festival.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw.festival.model.Attore;
import it.uniroma3.siw.festival.model.Festival;
import it.uniroma3.siw.festival.model.Film;
import it.uniroma3.siw.festival.model.Proiezione;
import it.uniroma3.siw.festival.model.Recensione;
import it.uniroma3.siw.festival.model.Regista;
import it.uniroma3.siw.festival.model.Ruolo;
import it.uniroma3.siw.festival.model.Sala;
import it.uniroma3.siw.festival.model.StatoProiezione;
import it.uniroma3.siw.festival.model.Utente;
import it.uniroma3.siw.festival.repository.AttoreRepository;
import it.uniroma3.siw.festival.repository.FestivalRepository;
import it.uniroma3.siw.festival.repository.FilmRepository;
import it.uniroma3.siw.festival.repository.ProiezioneRepository;
import it.uniroma3.siw.festival.repository.RecensioneRepository;
import it.uniroma3.siw.festival.repository.RegistaRepository;
import it.uniroma3.siw.festival.repository.SalaRepository;
import it.uniroma3.siw.festival.repository.UtenteRepository;

/**
 * Popola il database con dati di esempio all'avvio, solo nel profilo "dev" e solo se il DB è vuoto.
 * Le date dei festival sono relative a oggi, così la demo funziona in qualunque giorno:
 * Venezia è già concluso (proiezioni COMPLETED), Roma e Cannes sono futuri (proiezioni SCHEDULED).
 */
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // Fasce orarie: la durata massima dei film del seed (229') sta in 4 ore, quindi non si sovrappongono
    private static final LocalTime[] FASCE = { LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(18, 0) };

    private final FestivalRepository festivalRepository;
    private final FilmRepository filmRepository;
    private final RegistaRepository registaRepository;
    private final AttoreRepository attoreRepository;
    private final SalaRepository salaRepository;
    private final ProiezioneRepository proiezioneRepository;
    private final RecensioneRepository recensioneRepository;
    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(FestivalRepository festivalRepository, FilmRepository filmRepository,
            RegistaRepository registaRepository, AttoreRepository attoreRepository, SalaRepository salaRepository,
            ProiezioneRepository proiezioneRepository, RecensioneRepository recensioneRepository,
            UtenteRepository utenteRepository, PasswordEncoder passwordEncoder) {
        this.festivalRepository = festivalRepository;
        this.filmRepository = filmRepository;
        this.registaRepository = registaRepository;
        this.attoreRepository = attoreRepository;
        this.salaRepository = salaRepository;
        this.proiezioneRepository = proiezioneRepository;
        this.recensioneRepository = recensioneRepository;
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Una sola transazione: o il seed va a buon fine per intero, o non viene salvato nulla
    @Override
    @Transactional
    public void run(String... args) {
        if (festivalRepository.count() > 0) {
            log.info("Database già popolato: DataInitializer non eseguito");
            return;
        }

        // --- Registi ---
        Regista coppola = regista("Francis Ford", "Coppola", "1939-04-07", "Statunitense");
        Regista fellini = regista("Federico", "Fellini", "1920-01-20", "Italiana");
        Regista leone = regista("Sergio", "Leone", "1929-01-03", "Italiana");
        Regista scorsese = regista("Martin", "Scorsese", "1942-11-17", "Statunitense");
        Regista tarantino = regista("Quentin", "Tarantino", "1963-03-27", "Statunitense");
        Regista tornatore = regista("Giuseppe", "Tornatore", "1956-05-27", "Italiana");
        Regista benigniRegista = regista("Roberto", "Benigni", "1952-10-27", "Italiana");
        Regista sorrentino = regista("Paolo", "Sorrentino", "1970-05-31", "Italiana");
        Regista bong = regista("Joon-ho", "Bong", "1969-09-14", "Sudcoreana");
        Regista cuaron = regista("Alfonso", "Cuarón", "1961-11-28", "Messicana");
        Regista polanski = regista("Roman", "Polański", "1933-08-18", "Francese");
        Regista jeunet = regista("Jean-Pierre", "Jeunet", "1953-09-03", "Francese");
        Regista campion = regista("Jane", "Campion", "1954-04-30", "Neozelandese");
        Regista nolan = regista("Christopher", "Nolan", "1970-07-30", "Britannica");
        Regista garrone = regista("Matteo", "Garrone", "1968-10-15", "Italiana");
        Regista hamaguchi = regista("Ryūsuke", "Hamaguchi", "1978-12-16", "Giapponese");

        // --- Attori ---
        Attore brando = attore("Marlon", "Brando", "1924-04-03", "Statunitense");
        Attore pacino = attore("Al", "Pacino", "1940-04-25", "Statunitense");
        Attore sheen = attore("Martin", "Sheen", "1940-08-03", "Statunitense");
        Attore mastroianni = attore("Marcello", "Mastroianni", "1924-09-28", "Italiana");
        Attore ekberg = attore("Anita", "Ekberg", "1931-09-29", "Svedese");
        Attore cardinale = attore("Claudia", "Cardinale", "1938-04-15", "Italiana");
        Attore eastwood = attore("Clint", "Eastwood", "1930-05-31", "Statunitense");
        Attore wallach = attore("Eli", "Wallach", "1915-12-07", "Statunitense");
        Attore deNiro = attore("Robert", "De Niro", "1943-08-17", "Statunitense");
        Attore foster = attore("Jodie", "Foster", "1962-11-19", "Statunitense");
        Attore travolta = attore("John", "Travolta", "1954-02-18", "Statunitense");
        Attore thurman = attore("Uma", "Thurman", "1970-04-29", "Statunitense");
        Attore jackson = attore("Samuel L.", "Jackson", "1948-12-21", "Statunitense");
        Attore noiret = attore("Philippe", "Noiret", "1930-10-01", "Francese");
        Attore cascio = attore("Salvatore", "Cascio", "1979-11-07", "Italiana");
        Attore benigniAttore = attore("Roberto", "Benigni", "1952-10-27", "Italiana");
        Attore braschi = attore("Nicoletta", "Braschi", "1960-04-19", "Italiana");
        Attore servillo = attore("Toni", "Servillo", "1959-01-25", "Italiana");
        Attore songKangHo = attore("Kang-ho", "Song", "1967-01-17", "Sudcoreana");
        Attore aparicio = attore("Yalitza", "Aparicio", "1993-12-11", "Messicana");
        Attore brody = attore("Adrien", "Brody", "1973-04-14", "Statunitense");
        Attore tautou = attore("Audrey", "Tautou", "1976-08-09", "Francese");
        Attore hunter = attore("Holly", "Hunter", "1958-03-20", "Statunitense");
        Attore keitel = attore("Harvey", "Keitel", "1939-05-13", "Statunitense");
        Attore diCaprio = attore("Leonardo", "DiCaprio", "1974-11-11", "Statunitense");
        Attore nishijima = attore("Hidetoshi", "Nishijima", "1971-03-29", "Giapponese");

        // --- Film e cast (le interpretazioni si salvano col film grazie a cascade = ALL) ---
        Film padrino = film("Il Padrino", 1972, 175, "Drammatico", "Stati Uniti", coppola);
        padrino.addInterpretazione(brando, "Don Vito Corleone");
        padrino.addInterpretazione(pacino, "Michael Corleone");
        Film apocalypse = film("Apocalypse Now", 1979, 147, "Guerra", "Stati Uniti", coppola);
        apocalypse.addInterpretazione(brando, "Colonnello Walter E. Kurtz");
        apocalypse.addInterpretazione(sheen, "Capitano Benjamin L. Willard");
        Film dolceVita = film("La dolce vita", 1960, 174, "Drammatico", "Italia", fellini);
        dolceVita.addInterpretazione(mastroianni, "Marcello Rubini");
        dolceVita.addInterpretazione(ekberg, "Sylvia");
        Film ottoEMezzo = film("8½", 1963, 138, "Drammatico", "Italia", fellini);
        ottoEMezzo.addInterpretazione(mastroianni, "Guido Anselmi");
        ottoEMezzo.addInterpretazione(cardinale, "Claudia");
        Film buonoBrutto = film("Il buono, il brutto, il cattivo", 1966, 178, "Western", "Italia", leone);
        buonoBrutto.addInterpretazione(eastwood, "Il Biondo");
        buonoBrutto.addInterpretazione(wallach, "Tuco");
        Film americA = film("C'era una volta in America", 1984, 229, "Drammatico", "Italia", leone);
        americA.addInterpretazione(deNiro, "David \"Noodles\" Aaronson");
        Film taxiDriver = film("Taxi Driver", 1976, 114, "Drammatico", "Stati Uniti", scorsese);
        taxiDriver.addInterpretazione(deNiro, "Travis Bickle");
        taxiDriver.addInterpretazione(foster, "Iris");
        Film wolf = film("The Wolf of Wall Street", 2013, 180, "Commedia", "Stati Uniti", scorsese);
        wolf.addInterpretazione(diCaprio, "Jordan Belfort");
        Film pulpFiction = film("Pulp Fiction", 1994, 154, "Crime", "Stati Uniti", tarantino);
        pulpFiction.addInterpretazione(travolta, "Vincent Vega");
        pulpFiction.addInterpretazione(thurman, "Mia Wallace");
        pulpFiction.addInterpretazione(jackson, "Jules Winnfield");
        Film cinemaParadiso = film("Nuovo Cinema Paradiso", 1988, 155, "Drammatico", "Italia", tornatore);
        cinemaParadiso.addInterpretazione(noiret, "Alfredo");
        cinemaParadiso.addInterpretazione(cascio, "Salvatore \"Totò\" Di Vita (bambino)");
        Film vitaBella = film("La vita è bella", 1997, 116, "Commedia", "Italia", benigniRegista);
        vitaBella.addInterpretazione(benigniAttore, "Guido Orefice");
        vitaBella.addInterpretazione(braschi, "Dora");
        Film grandeBellezza = film("La grande bellezza", 2013, 141, "Drammatico", "Italia", sorrentino);
        grandeBellezza.addInterpretazione(servillo, "Jep Gambardella");
        Film parasite = film("Parasite", 2019, 132, "Thriller", "Corea del Sud", bong);
        parasite.addInterpretazione(songKangHo, "Kim Ki-taek");
        Film roma = film("Roma", 2018, 135, "Drammatico", "Messico", cuaron);
        roma.addInterpretazione(aparicio, "Cleo");
        Film pianista = film("Il pianista", 2002, 150, "Drammatico", "Francia", polanski);
        pianista.addInterpretazione(brody, "Władysław Szpilman");
        Film amelie = film("Il favoloso mondo di Amélie", 2001, 122, "Commedia", "Francia", jeunet);
        amelie.addInterpretazione(tautou, "Amélie Poulain");
        Film lezioniPiano = film("Lezioni di piano", 1993, 121, "Drammatico", "Nuova Zelanda", campion);
        lezioniPiano.addInterpretazione(hunter, "Ada McGrath");
        lezioniPiano.addInterpretazione(keitel, "George Baines");
        Film inception = film("Inception", 2010, 148, "Fantascienza", "Stati Uniti", nolan);
        inception.addInterpretazione(diCaprio, "Dom Cobb");
        Film gomorra = film("Gomorra", 2008, 137, "Crime", "Italia", garrone);
        gomorra.addInterpretazione(servillo, "Franco");
        Film driveMyCar = film("Drive My Car", 2021, 179, "Drammatico", "Giappone", hamaguchi);
        driveMyCar.addInterpretazione(nishijima, "Yūsuke Kafuku");

        registaRepository.saveAll(List.of(coppola, fellini, leone, scorsese, tarantino, tornatore, benigniRegista,
                sorrentino, bong, cuaron, polanski, jeunet, campion, nolan, garrone, hamaguchi));
        attoreRepository.saveAll(List.of(brando, pacino, sheen, mastroianni, ekberg, cardinale, eastwood, wallach,
                deNiro, foster, travolta, thurman, jackson, noiret, cascio, benigniAttore, braschi, servillo,
                songKangHo, aparicio, brody, tautou, hunter, keitel, diCaprio, nishijima));
        filmRepository.saveAll(List.of(padrino, apocalypse, dolceVita, ottoEMezzo, buonoBrutto, americA, taxiDriver,
                wolf, pulpFiction, cinemaParadiso, vitaBella, grandeBellezza, parasite, roma, pianista, amelie,
                lezioniPiano, inception, gomorra, driveMyCar));

        // --- Sale ---
        List<Sala> sale = salaRepository.saveAll(List.of(
                new Sala("Sala Grande", "Lungomare Marconi 30, Venezia Lido", 1032),
                new Sala("Sala Darsena", "Lungomare Marconi 30, Venezia Lido", 1409),
                new Sala("Sala Sinopoli", "Viale Pietro de Coubertin 30, Roma", 1200),
                new Sala("Sala Petrassi", "Viale Pietro de Coubertin 30, Roma", 700),
                new Sala("Cinema Lumière", "Via Azzo Gardino 65, Bologna", 400)));

        // --- Festival (date relative a oggi) e partecipazioni dei film ---
        LocalDate oggi = LocalDate.now();
        Festival venezia = festival("Mostra Internazionale d'Arte Cinematografica", "Venezia",
                oggi.minusDays(50), oggi.minusDays(40),
                "Il più antico festival cinematografico del mondo, al Lido di Venezia.");
        Festival festaRoma = festival("Festa del Cinema di Roma", "Roma", oggi.plusDays(10), oggi.plusDays(20),
                "Rassegna internazionale all'Auditorium Parco della Musica.");
        Festival cannes = festival("Festival de Cannes", "Cannes", oggi.plusDays(30), oggi.plusDays(40),
                "Il festival della Croisette, con la Palma d'oro.");

        // Il Padrino e La grande bellezza partecipano a due festival: è la relazione molti-a-molti
        List.of(padrino, dolceVita, ottoEMezzo, buonoBrutto, grandeBellezza, roma, driveMyCar)
                .forEach(venezia::addFilm);
        List.of(cinemaParadiso, americA, amelie, inception, wolf, padrino, grandeBellezza)
                .forEach(festaRoma::addFilm);
        List.of(pulpFiction, parasite, pianista, lezioniPiano, taxiDriver, apocalypse, vitaBella, gomorra)
                .forEach(cannes::addFilm);
        festivalRepository.saveAll(List.of(venezia, festaRoma, cannes));

        // --- Proiezioni: due per film, distribuite su giorni, fasce e sale senza sovrapposizioni ---
        List<Proiezione> proiezioni = new ArrayList<>();
        proiezioni.addAll(programma(venezia, sale, StatoProiezione.COMPLETED));
        proiezioni.addAll(programma(festaRoma, sale, StatoProiezione.SCHEDULED));
        proiezioni.addAll(programma(cannes, sale, StatoProiezione.SCHEDULED));
        proiezioni.get(proiezioni.size() - 1).setStato(StatoProiezione.CANCELLED); // un esempio di annullata
        proiezioneRepository.saveAll(proiezioni);

        // --- Utenti (password cifrate con BCrypt) ---
        Utente admin = new Utente("admin", passwordEncoder.encode("admin"), Ruolo.ADMIN);
        Utente user1 = new Utente("user1", passwordEncoder.encode("user1"), Ruolo.USER);
        Utente user2 = new Utente("user2", passwordEncoder.encode("user2"), Ruolo.USER);
        utenteRepository.saveAll(List.of(admin, user1, user2));

        // --- Recensioni (voto 1-5) ---
        recensioneRepository.saveAll(List.of(
                new Recensione("Un capolavoro assoluto, Brando è monumentale.", 5, oggi.minusDays(45), padrino, user1),
                new Recensione("Lungo ma ipnotico: ogni scena è memorabile.", 4, oggi.minusDays(44), padrino, user2),
                new Recensione("Roma come non l'avevo mai vista, Servillo straordinario.", 5, oggi.minusDays(42),
                        grandeBellezza, user1),
                new Recensione("Bello ma a tratti troppo compiaciuto.", 3, oggi.minusDays(41), grandeBellezza, user2),
                new Recensione("Cambia genere ogni mezz'ora e funziona sempre.", 5, oggi.minusDays(5), parasite, user2),
                new Recensione("Dialoghi indimenticabili, montaggio geniale.", 4, oggi.minusDays(3), pulpFiction,
                        user1)));

        log.info("Dati di esempio inseriti: 3 festival, 20 film, {} proiezioni, 3 utenti", proiezioni.size());
    }

    /*
     * Assegna a ogni film del festival due proiezioni. Il contatore k individua una combinazione
     * (giorno, fascia, sala) sempre diversa, quindi non ci sono sovrapposizioni nella stessa sala.
     * I festival hanno periodi disgiunti, quindi possono condividere le sale.
     */
    private List<Proiezione> programma(Festival festival, List<Sala> sale, StatoProiezione stato) {
        int giorni = (int) (festival.getDataFine().toEpochDay() - festival.getDataInizio().toEpochDay()) + 1;
        List<Film> films = festival.getFilms().stream()
                .sorted(Comparator.comparing(Film::getTitolo))
                .toList();
        List<Proiezione> risultato = new ArrayList<>();
        int k = 0;
        for (Film film : films) {
            for (int n = 0; n < 2; n++, k++) {
                LocalDate data = festival.getDataInizio().plusDays(k % giorni);
                LocalTime ora = FASCE[(k / giorni) % FASCE.length];
                Sala sala = sale.get((k / (giorni * FASCE.length)) % sale.size());

                Proiezione proiezione = new Proiezione();
                festival.addProiezione(proiezione);
                film.addProiezione(proiezione);
                sala.addProiezione(proiezione);
                proiezione.impostaOrario(data, ora);
                proiezione.setStato(stato);
                risultato.add(proiezione);
            }
        }
        return risultato;
    }

    private Regista regista(String nome, String cognome, String dataNascita, String nazionalita) {
        return new Regista(nome, cognome, LocalDate.parse(dataNascita), nazionalita);
    }

    private Attore attore(String nome, String cognome, String dataNascita, String nazionalita) {
        return new Attore(nome, cognome, LocalDate.parse(dataNascita), nazionalita);
    }

    private Film film(String titolo, int anno, int durata, String genere, String paese, Regista regista) {
        return new Film(titolo, anno, durata, genere, paese, regista);
    }

    private Festival festival(String nome, String citta, LocalDate inizio, LocalDate fine, String descrizione) {
        return new Festival(nome, inizio.getYear(), citta, inizio, fine, descrizione);
    }
}
