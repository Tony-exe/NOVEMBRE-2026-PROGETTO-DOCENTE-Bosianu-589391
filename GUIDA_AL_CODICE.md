# Guida al codice

Questa guida spiega il progetto dall'inizio alla fine, per poterlo discutere all'orale e modificarlo dal vivo.
I percorsi dei file Java sono relativi a `src/main/java/it/uniroma3/siw/festival/` (es. `service/ProiezioneService.java`);
quelli dei test a `src/test/java/it/uniroma3/siw/festival/`.

**Indice**
1. [Visione d'insieme](#1-visione-dinsieme)
2. [Struttura del progetto](#2-struttura-del-progetto)
3. [Modello dei dati](#3-modello-dei-dati)
4. [Strategie di fetch](#4-strategie-di-fetch)
5. [Service layer e transazioni](#5-service-layer-e-transazioni)
6. [Sicurezza](#6-sicurezza)
7. [API REST](#7-api-rest)
8. [Frontend](#8-frontend)
9. [Validazione ed errori](#9-validazione-ed-errori)
10. [Esperimento sulle strategie di fetch](#10-esperimento-sulle-strategie-di-fetch)
11. [Come modificare il progetto](#11-come-modificare-il-progetto)
12. [Glossario](#12-glossario)

---

## 1. Visione d'insieme

### Cosa fa l'applicazione
Gestisce uno o più festival cinematografici. Per ogni festival registra i film partecipanti, il loro cast
(attori e personaggi), le sale e le proiezioni programmate. Ci sono tre tipi di utenti:
- **visitatore** (non autenticato): consulta festival, film, programma e recensioni;
- **utente registrato** (`USER`): scrive una recensione per film e modifica/elimina solo le proprie;
- **amministratore** (`ADMIN`): gestisce festival, film, cast, registi, attori, sale e proiezioni.

La maggior parte dell'interfaccia è in **Thymeleaf** (HTML generato dal server); due pagine sono in **React**
(dettaglio film con recensioni, programma delle proiezioni) e leggono i dati da **API REST** dello stesso backend.

### Architettura a livelli
```
Browser
  │  richiesta HTTP
  ▼
Spring Security ── filtri: sessione, CSRF, autenticazione, autorizzazione sugli URL
  ▼
Controller ──────── controller (Thymeleaf) oppure controller.api (REST)
  │                  riceve la richiesta, valida l'input, chiama UN metodo di service, prepara vista o DTO
  ▼
Service ─────────── casi d'uso e regole di business; @Transactional; @PreAuthorize
  ▼
Repository ──────── Spring Data JPA: query (JOIN FETCH, @EntityGraph, lock)
  ▼
Entità JPA ──► Hibernate ──► SQL ──► PostgreSQL
```
Regole rispettate in tutto il codice:
- i controller **non** usano mai i repository: passano sempre da un service;
- **tutta** la logica (controlli, regole, coordinamento di più repository) è nei service;
- le entità **non** escono mai in JSON: le API restituiscono DTO;
- `spring.jpa.open-in-view=false`: la sessione di Hibernate vive solo dentro le transazioni dei service.

### Percorso di una richiesta Thymeleaf: `GET /festivals/2/films`
1. Il browser chiede `/festivals/2/films`. La catena di filtri di Spring Security lascia passare (pagina pubblica,
   `anyRequest().permitAll()` in `security/SecurityConfig.java`).
2. Spring MVC trova il metodo `films` di `controller/FestivalController.java` (`@GetMapping("/festivals/{id}/films")`)
   e converte `{id}` in `Long id = 2`.
3. Il controller chiama `festivalService.trova(2)` e `filmService.filmDelFestival(2)`.
4. `FilmService.filmDelFestival` (in una transazione di sola lettura) controlla che il festival esista e chiama
   `FilmRepository.findByFestivalIdWithRegista(2)`, che esegue **una** query:
   `select f from Film f join fetch f.regista join f.festivals fe where fe.id = :festivalId`.
   Alla fine del metodo la transazione si chiude; le entità restano in memoria, con il regista già caricato.
5. Il controller mette le entità nel `Model` e restituisce il nome della vista `"festivals/films"`.
6. Thymeleaf rende `templates/festivals/films.html`: `${film.regista.nome}` funziona perché il regista è già
   caricato. Senza il JOIN FETCH avremmo una `LazyInitializationException` (sessione chiusa).
7. Se il festival non esiste, `trova` lancia `EntityNotFoundException`, che `exception/MvcExceptionHandler.java`
   trasforma nella pagina `error/404.html` con status 404.

### Percorso di una richiesta React + REST: inserimento di una recensione
1. In `/app/movies/1` l'utente compila il form (`frontend/src/components/ReviewForm.jsx`) e preme "Pubblica".
2. `ReviewsSection.crea` chiama `api.post('/api/movies/1/reviews', { testo, voto })` (`frontend/src/api.js`), che
   invia JSON con il cookie di sessione e l'header `X-XSRF-TOKEN` letto dal cookie `XSRF-TOKEN`.
3. Spring Security: il `CsrfFilter` confronta header e cookie; l'`AuthorizationFilter` verifica la regola
   `/api/movies/*/reviews → hasAnyRole('USER','ADMIN')`. Se l'utente non è loggato, `ApiAuthenticationEntryPoint`
   risponde **401** in JSON.
4. `controller/api/MovieApiController.crea` riceve il corpo come `RecensioneForm` con `@Valid`: se non è valido,
   `ApiExceptionHandler` risponde **400** con l'elenco dei campi.
5. Il controller chiama `recensioneService.crea(1, authentication.getName(), form)`. Il proxy di Spring applica
   `@PreAuthorize` (lo username passato deve essere quello autenticato) e apre la transazione.
6. Il service carica film e utente, controlla che non esista già una recensione (altrimenti
   `RecensioneDuplicataException` → **409**), salva con `saveAndFlush` e restituisce l'entità.
7. Il controller costruisce `RecensioneDto` e risponde **201 Created** con header `Location: /api/reviews/{id}`.
8. React ricarica l'elenco (`GET /api/movies/1/reviews`) e mostra il messaggio di conferma.

---

## 2. Struttura del progetto

```
pom.xml                  dipendenze Maven; frontend-maven-plugin compila React durante la build
frontend/                progetto React (Vite)
src/main/java/.../festival/
  FestivalApplication    classe main (@SpringBootApplication)
  model/                 entità JPA
  repository/            interfacce Spring Data JPA
  service/               casi d'uso
  controller/            controller Thymeleaf pubblici + ReactForwardController
  controller/admin/      controller Thymeleaf dell'area amministrativa
  controller/api/        controller REST
  dto/form/              form object (Thymeleaf e corpo JSON delle scritture)
  dto/api/               DTO JSON delle risposte
  exception/             eccezioni di dominio e gestori centralizzati
  security/              Spring Security
  config/                dati di esempio, OpenAPI
src/main/resources/
  application.properties, application-dev.properties
  templates/             pagine Thymeleaf
  static/css/app.css     stile comune (Thymeleaf e React)
src/test/                test, profili test ed experiment
```

### `model` – entità
| Classe | Ruolo |
|---|---|
| `Festival` | festival (nome, anno, città, date, descrizione); lato proprietario della molti-a-molti con `Film` |
| `Film` | film (titolo, anno, durata, genere, paese); regista, cast, festival, proiezioni, recensioni |
| `Regista` | regista di un film |
| `Attore` | attore |
| `Interpretazione` | entità associativa Film–Attore con il `personaggio` |
| `Sala` | sala (nome, indirizzo, capienza) |
| `Proiezione` | proiezione (data, ora, `oraFine` calcolata, stato) di un film in una sala per un festival |
| `Recensione` | recensione (testo, voto 1–5, data) di un utente su un film |
| `Utente` | utente (username, hash BCrypt della password, ruolo) |
| `StatoProiezione`, `Ruolo` | enum salvati come stringa |

### `repository`
Un'interfaccia per entità che estende `JpaRepository<Entità, Long>` (CRUD già pronti: `findById`, `save`, `delete`...).
I metodi aggiuntivi sono query derivate dal nome (`findByUsername`) o `@Query` in JPQL. I più importanti:
- `FilmRepository`: le tre varianti per i film di un festival (`findByFestivalId`, `findByFestivalIdWithRegista`,
  `findByFestivalIdWithEntityGraph`), `findByIdWithRegistaAndCast`, la ricerca paginata `cerca`,
  `findNonPartecipantiAlFestival`, `findAllWithRegista`;
- `ProiezioneRepository`: `findProgramma`, `findProgrammaDelGiorno`, `findByFilmIdWithSalaAndFestival`,
  `findByIdWithDettagli`, `esisteSovrapposizione`, `esistonoProiezioniFuoriPeriodo`, `findByFestivalIdAndFilmId`;
- `SalaRepository.findByIdForUpdate`: lettura con lock pessimistico;
- `FestivalRepository`: `findByIdWithFilms`, `findByFilmId`, `partecipaAlFestival`;
- `RecensioneRepository`: `findByFilmIdWithUtente`, `findByIdWithUtenteAndFilm`, `existsByFilmIdAndUtenteId`, `mediaVoti`.

### `service`
| Classe | Cosa fa |
|---|---|
| `FestivalService` | elenco/dettaglio, crea/modifica, aggiunge e rimuove film (annullando le proiezioni) |
| `FilmService` | ricerca, dettaglio completo (`DettaglioFilm`), crea/modifica, cast |
| `RegistaService`, `AttoreService`, `SalaService` | elenco, dettaglio, crea, modifica |
| `ProiezioneService` | programma di un festival, **programmazione** con tutti i controlli, modifica, annullamento, completamento |
| `RecensioneService` | recensioni e media di un film, crea/modifica/elimina con controllo di proprietà |
| `UtenteService` | registrazione (BCrypt, ruolo USER) |
| `DettaglioFilm` | record che raggruppa le entità caricate per il dettaglio film |

### `controller`, `controller.admin`, `controller.api`
| Classe | URL |
|---|---|
| `HomeController` | `/` |
| `FestivalController` | `/festivals`, `/festivals/{id}`, `/festivals/{id}/films` |
| `AuthController` | `/login` (solo la pagina; il POST lo gestisce Spring Security), `/register` |
| `ReactForwardController` | `/app`, `/app/movies/{id}`, `/app/festivals/{id}/screenings` → `index.html` di React |
| `admin/AdminController` | `/admin` |
| `admin/AdminFestivalController` | `/admin/festivals/**` (festival e film partecipanti) |
| `admin/AdminFilmController` | `/admin/films/**` (film e cast) |
| `admin/AdminPersoneController` | `/admin/registi/**`, `/admin/attori/**` |
| `admin/AdminSalaController` | `/admin/sale/**` |
| `admin/AdminProiezioneController` | `/admin/festivals/{id}/proiezioni`, `/admin/proiezioni/**` |
| `api/FestivalApiController`, `api/MovieApiController`, `api/ReviewApiController`, `api/MeApiController` | `/api/**` (§7) |

### `dto`
- `dto/form`: `FestivalForm` (con il vincolo di classe `@PeriodoValido` e il suo `PeriodoValidoValidator`), `FilmForm`,
  `PersonaForm` (registi e attori), `SalaForm`, `ProiezioneForm`, `InterpretazioneForm`, `RecensioneForm`,
  `RegistrazioneForm`. Hanno i vincoli di Bean Validation e metodi statici `da(entità)` per precompilare i form di modifica.
- `dto/api`: record Java per il JSON (`FestivalDto`, `FilmDto`, `FilmDettaglioDto`, `RegistaDto`, `ProiezioneDto`,
  `RecensioneDto`, `RecensioniFilmDto`, `PaginaDto`, `UtenteCorrenteDto`), ognuno con un metodo statico di conversione.

### `exception`, `security`, `config`
- `exception`: `EntityNotFoundException`, `ConflittoProgrammazioneException`, `RecensioneDuplicataException`,
  `OperazioneNonAutorizzataException`, `RegolaBusinessException`, `UsernameGiaUsatoException`;
  `MvcExceptionHandler` (pagine HTML), `ApiExceptionHandler` (ProblemDetail).
- `security`: `SecurityConfig` (catena di filtri), `UtenteDetailsService`, `PasswordConfig` (BCrypt),
  `ApiAuthenticationEntryPoint` (401), `ApiAccessDeniedHandler` (403), `CsrfCookieFilter`.
- `config`: `DataInitializer` (dati di esempio, profilo `dev`), `OpenApiConfig` (titolo della documentazione Swagger).

---

## 3. Modello dei dati

### Diagramma
```
Regista 1 ───< Film >─── M:N ───< Festival          (tabella festival_film; lato proprietario: Festival)
               │ 1  1 │                │ 1
               │      │                │
     N Interpretazione│N        N Proiezione >── 1 Sala
               │ N    │                 (anche Film 1 ──< N Proiezione)
          1 Attore    │
                      └──< N Recensione >── 1 Utente
```

### Associazioni e perché sono mappate così
**Festival ↔ Film (molti-a-molti).** Un festival presenta più film, un film partecipa a più festival.
```java
// Festival.java – lato proprietario
@ManyToMany
@JoinTable(name = "festival_film",
        joinColumns = @JoinColumn(name = "festival_id"),
        inverseJoinColumns = @JoinColumn(name = "film_id"))
private Set<Film> films = new HashSet<>();

// Film.java – lato inverso
@ManyToMany(mappedBy = "films")
private Set<Festival> festivals = new HashSet<>();
```
- In un'associazione bidirezionale un solo lato "comanda" la scrittura nel DB: il **lato proprietario**. Qui è
  `Festival.films`: aggiungere un film a quella collezione produce l'`INSERT` in `festival_film`. Modificare solo
  `Film.festivals` (il lato con `mappedBy`) **non** scrive nulla.
- Per questo esistono i metodi helper `Festival.addFilm` / `removeFilm`, che aggiornano entrambi i lati e tengono
  coerente il modello in memoria.
- `Set` e non `List`: rimuovendo un elemento da una `List` senza indice (*bag*) Hibernate cancella tutte le righe
  della join table e le reinserisce; con un `Set` cancella solo la riga interessata.

**Film → Regista (molti-a-uno).**
```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "regista_id", nullable = false)
private Regista regista;
```
La chiave esterna `regista_id` sta nella tabella `film`, quindi il lato proprietario è `Film.regista`.
`Regista.films` è il lato inverso (`@OneToMany(mappedBy = "regista")`).

**Film ↔ Attore tramite Interpretazione.** Il legame ha un attributo (il personaggio), quindi non può essere una
semplice `@ManyToMany`: diventa un'entità con due `@ManyToOne` (verso `Film` e verso `Attore`) e un vincolo
`unique (film_id, attore_id, personaggio)`. Sul lato `Film` la collezione ha
`cascade = CascadeType.ALL, orphanRemoval = true`: il cast è "parte" del film; salvando il film si salvano le
interpretazioni nuove, togliendone una dalla lista (`Film.removeInterpretazione`) la riga viene cancellata.

**Proiezione → Festival, Film, Sala.** Tre `@ManyToOne(LAZY, optional = false)`: una proiezione appartiene
sempre a un festival, riguarda un film e si svolge in una sala. Sui lati inversi (`Festival.proiezioni`,
`Film.proiezioni`, `Sala.proiezioni`) c'è `mappedBy` e i metodi `addProiezione`.
La proiezione salva anche **`oraFine`** = ora + durata del film, calcolata da `Proiezione.impostaOrario`: è un dato
ridondante, ma rende la query di sovrapposizione un confronto tra colonne, coperto dall'indice
`idx_proiezione_sala_data (sala_id, data)`.

**Recensione → Film, Utente.** Due `@ManyToOne(LAZY)` e un vincolo `unique (film_id, utente_id)`: al massimo una
recensione per utente per film. Il vincolo nel DB garantisce la regola anche con richieste concorrenti.

**Cascade.** Solo su `Film.interpretazioni`. Proiezioni e recensioni hanno vita propria: cancellare un film non
deve cancellarle "per sbaglio" (e infatti nel progetto non si cancellano film).

### Convenzioni comuni a tutte le entità
- `@Id @GeneratedValue(strategy = IDENTITY)`: id generato dal DB (colonna `bigint` con identity).
- `LocalDate` e `LocalTime` per date e orari; enum con `@Enumerated(EnumType.STRING)` (nel DB il nome, non la
  posizione: aggiungere o riordinare valori non corrompe i dati).
- `equals`/`hashCode` basati sull'id (vedi `Festival.java`): due entità sono uguali solo se hanno lo stesso id non
  nullo; un'entità nuova è uguale solo a se stessa; `hashCode` è costante per la classe, così non cambia quando l'id
  viene assegnato al persist e l'oggetto resta trovabile in un `HashSet`. Si usa `other.getId()` perché `other`
  potrebbe essere un proxy Hibernate (i cui campi sono vuoti).
- Vincoli di Bean Validation sugli attributi (`@NotBlank`, `@Min`, `@Max`, `@Past`, `@Size`...): vengono verificati
  da Hibernate prima di ogni `INSERT`/`UPDATE` e **tradotti anche nello schema** (vedi sotto).

### Le tabelle che Hibernate genera
Con `ddl-auto=update` (profilo `dev`) Hibernate crea lo schema dalle entità. Nomi in *snake_case*
(`dataInizio` → `data_inizio`).

| Tabella | Colonne | Vincoli |
|---|---|---|
| `festival` | id, nome, anno, citta, data_inizio, data_fine, descrizione (2000) | PK; `CHECK anno between 1900 and 2100` |
| `film` | id, titolo, anno, durata, genere, paese_produzione, regista_id | PK; FK regista; `CHECK durata 1–600`, `CHECK anno 1888–2100` |
| `festival_film` | festival_id, film_id | PK composta (festival_id, film_id); FK verso festival e film |
| `regista`, `attore` | id, nome, cognome, data_nascita, nazionalita | PK |
| `interpretazione` | id, personaggio, film_id, attore_id | PK; FK; `UNIQUE (film_id, attore_id, personaggio)` |
| `sala` | id, nome, indirizzo, capienza | PK; `CHECK capienza >= 1` |
| `proiezione` | id, data, ora, ora_fine, stato, festival_id, film_id, sala_id | PK; FK; `CHECK stato in (...)`; indice (sala_id, data) |
| `recensione` | id, testo (2000), voto, data, film_id, utente_id | PK; FK; `UNIQUE (film_id, utente_id)`; `CHECK voto 1–5` |
| `utente` | id, username (50), password (100), ruolo | PK; `UNIQUE (username)`; `CHECK ruolo in ('USER','ADMIN')` |

I `NOT NULL` vengono da `nullable = false`, `optional = false`, `@NotNull` e `@NotBlank`; i `CHECK` su numeri da
`@Min`/`@Max`; quelli sugli stati dagli enum. Per vedere lo schema reale: `\d proiezione` in `psql`.

---

## 4. Strategie di fetch

### LAZY ed EAGER
- **EAGER**: l'associazione viene caricata sempre insieme all'entità. Default JPA per `@ManyToOne` e `@OneToOne`.
- **LAZY**: al posto dell'oggetto associato Hibernate mette un **proxy** (o una collezione non inizializzata); la
  query parte solo al primo accesso a un suo campo. Default per `@OneToMany` e `@ManyToMany`.

Nel progetto **tutto è LAZY**, anche le `@ManyToOne` (dichiarate `fetch = FetchType.LAZY`). EAGER è comodo ma
non si può "spegnere" quando non serve: ogni film caricherebbe sempre il regista, anche dove non viene mostrato,
e le associazioni EAGER in cascata possono portare a molte query o join inutili. Con LAZY si decide caso per caso,
nel repository, cosa caricare.

### Il problema N+1, con un esempio del progetto
```java
List<Film> films = filmRepository.findByFestivalId(festivalId);   // 1 query: i 100 film
for (Film film : films) {
    film.getRegista().getCognome();                                 // 1 query PER FILM: 100 query
}
```
`findByFestivalId` carica solo i film; `getRegista()` restituisce un proxy e `getCognome()` lo inizializza con
`select ... from regista where id = ?`. Totale: **1 + N** query. Con 100 film sono 101 andate e ritorno verso il DB;
con 100.000 film, 100.001. È il caso esatto misurato dall'esperimento (§10).

### Le soluzioni
**JOIN FETCH** (`FilmRepository.findByFestivalIdWithRegista`):
```java
@Query("select f from Film f join fetch f.regista join f.festivals fe where fe.id = :festivalId order by f.titolo")
```
`join fetch` dice a Hibernate di caricare i registi nella stessa SELECT, con una join: **1 query**.

**@EntityGraph** (`FilmRepository.findByFestivalIdWithEntityGraph`):
```java
@EntityGraph(attributePaths = "regista")
@Query("select f from Film f join f.festivals fe where fe.id = :festivalId order by f.titolo")
```
La query è quella "LAZY", ma il grafo chiede di caricare anche `regista`: Hibernate aggiunge la join. Stesso SQL
del JOIN FETCH, con la strategia dichiarata fuori dalla query (comodo con le query derivate dal nome).

**Batch fetching** (`@BatchSize` sull'entità, `hibernate.default_batch_fetch_size` globale o, come
nell'esperimento, `Session.setFetchBatchSize(25)`): la query resta LAZY, ma quando serve un proxy Hibernate
inizializza insieme fino a 25 proxy dello stesso tipo:
`select ... from regista where id = any (?)` (con PostgreSQL gli id sono passati come un unico array).
Risultato: **1 + ⌈N/25⌉** query. Utile quando non si può cambiare la query o per le collezioni.

### Più collezioni insieme: perché le carico separatamente
Il dettaglio di un film richiede cast, festival, proiezioni e recensioni: quattro collezioni. Un'unica query con
quattro `join fetch` di collezioni:
- con le `List` (bag) fallisce con **`MultipleBagFetchException`** (Hibernate non riesce a ricostruire più bag
  dallo stesso risultato);
- con i `Set` funzionerebbe, ma restituirebbe il **prodotto cartesiano**: righe = cast × festival × proiezioni ×
  recensioni (per un film con 10 attori, 2 festival, 6 proiezioni e 20 recensioni: 2.400 righe per un solo film).

Per questo `FilmService.dettaglio` esegue quattro query, ognuna con al massimo **una** collezione in fetch, tutte
nella stessa transazione:
1. `FilmRepository.findByIdWithRegistaAndCast` – film + regista + interpretazioni + attori;
2. `FestivalRepository.findByFilmId` – festival del film;
3. `ProiezioneRepository.findByFilmIdWithSalaAndFestival` – proiezioni con sala e festival (to-one: niente moltiplicazione);
4. `RecensioneRepository.findByFilmIdWithUtente` – recensioni con l'autore.

Le associazioni **to-one** in JOIN FETCH invece non moltiplicano le righe e si possono combinare liberamente
(`findProgramma` carica proiezione + film + sala) e paginare (`FilmRepository.cerca`).

### Perché `open-in-view` è disattivato
Con `spring.jpa.open-in-view=true` (default di Spring Boot) la sessione di Hibernate resta aperta fino alla fine
della richiesta, anche mentre Thymeleaf rende la pagina o Jackson serializza il JSON: ogni accesso LAZY dimenticato
genera silenziosamente query aggiuntive (spesso un N+1) fuori dal service e fuori da ogni transazione.
Con `false` (in `application.properties`), fuori dal service la sessione è chiusa e lo stesso errore diventa una
`LazyInitializationException` immediata: il problema emerge subito e si risolve dove va risolto, nel repository.
`controller/PagineMvcTest` rende tutte le pagine proprio per intercettare questi casi.

### Strategia per ogni associazione principale
| Associazione | Mapping | Caricata con |
|---|---|---|
| Film → Regista | `@ManyToOne(LAZY)` | JOIN FETCH (`findByFestivalIdWithRegista`, `findAllWithRegista`, `cerca`) o EntityGraph |
| Festival ↔ Film | `@ManyToMany` LAZY, `Set` | `findByIdWithFilms` (gestione film del festival), `findByFilmId` (festival di un film) |
| Film → Interpretazioni → Attore | `@OneToMany` + `@ManyToOne(LAZY)` | `findByIdWithRegistaAndCast` |
| Proiezione → Film, Sala, Festival | `@ManyToOne(LAZY)` | `findProgramma`, `findProgrammaDelGiorno`, `findByIdWithDettagli`, `findByFilmIdWithSalaAndFestival` |
| Recensione → Utente, Film | `@ManyToOne(LAZY)` | `findByFilmIdWithUtente`, `findByIdWithUtenteAndFilm` |

---

## 5. Service layer e transazioni

### Come funziona `@Transactional`
Spring crea un **proxy** attorno a ogni service. Quando un controller chiama un metodo, chiama in realtà il proxy,
che:
1. apre la transazione (o si unisce a una già aperta, propagazione `REQUIRED`);
2. applica `@PreAuthorize`, se presente;
3. chiama il metodo vero;
4. se il metodo termina normalmente fa **commit** (prima Hibernate esegue il *flush*: scrive nel DB tutte le
   modifiche alle entità *managed*); se esce con una `RuntimeException` fa **rollback**.

Conseguenze da sapere:
- **Le eccezioni checked non fanno rollback** per default: per questo tutte le eccezioni di dominio estendono
  `RuntimeException`.
- **Chiamate interne**: se un metodo del service ne chiama un altro della stessa classe (es. `FestivalService.modifica`
  chiama `trova`), la chiamata non passa dal proxy: le annotazioni del metodo chiamato non vengono applicate di
  nuovo. Qui non è un problema, perché il chiamante ha già la sua transazione.
- **Dirty checking**: in `FestivalService.modifica` non c'è nessun `save`. Il festival caricato è *managed*: al commit
  Hibernate confronta lo stato con quello letto e genera l'`UPDATE`.
- **`readOnly = true`**: ogni service ha `@Transactional(readOnly = true)` a livello di classe; i metodi che scrivono
  lo sovrascrivono con `@Transactional`. In sola lettura Hibernate salta flush e dirty checking (meno lavoro) e il
  codice dichiara l'intento.
- **Isolamento**: dove serve è indicato esplicitamente (`isolation = Isolation.READ_COMMITTED` in `ProiezioneService`).

### Cosa fa ogni service
- `FestivalService`: `elenco`, `trova`, `trovaConFilm`, `crea`, `modifica` (rifiuta un periodo che lascerebbe fuori
  proiezioni programmate), `aggiungiFilm`, `rimuoviFilm`.
- `FilmService`: `elenco`, `cerca` (filtri e pagina, dimensione massima 50), `filmDelFestival`, `filmAssociabili`,
  `trovaConCast`, `dettaglio`, `crea`, `modifica` (non si cambia la durata se ci sono proiezioni programmate),
  `aggiungiInterpretazione`, `rimuoviInterpretazione`.
- `ProiezioneService`: `programma`, `trova`, `programmaProiezione`, `modificaProiezione`, `annulla`, `segnaCompletata`.
- `RecensioneService`: `recensioniDelFilm`, `trova`, `mediaVoti`, `crea`, `modifica`, `elimina`.
- `UtenteService`: `trova`, `registra`. `RegistaService`, `AttoreService`, `SalaService`: CRUD senza eliminazione.

### `ProiezioneService.programmaProiezione`, passo per passo
È il caso d'uso che coinvolge più entità e più repository.
```java
@PreAuthorize("hasRole('ADMIN')")
@Transactional(isolation = Isolation.READ_COMMITTED)
public Proiezione programmaProiezione(Long festivalId, Long filmId, Long salaId, LocalDate data, LocalTime ora)
```
1. **Recupero di festival e film** con `findById`; se mancano → `EntityNotFoundException` (404).
2. **Regole**: il film deve partecipare al festival (`FestivalRepository.partecipaAlFestival`, una `count` senza
   caricare la collezione); la data deve essere nel periodo (`Festival.comprendeData`) e non nel passato
   (`verificaDataEOra`); la proiezione deve finire entro mezzanotte (`calcolaOraFine`). Violazioni →
   `RegolaBusinessException`.
3. **Lock pessimistico sulla sala**: `SalaRepository.findByIdForUpdate` ha `@Lock(PESSIMISTIC_WRITE)` e genera
   `select ... from sala where id = ? for update`. La riga della sala resta bloccata fino alla fine della transazione.
   Se la sala non esiste → 404.
4. **Verifica delle sovrapposizioni** (`ProiezioneRepository.esisteSovrapposizione`): stessa sala, stessa data, stato
   diverso da CANCELLED, `p.ora < :oraFine and p.oraFine > :ora`. Se esiste → `ConflittoProgrammazioneException` (409).
5. **Creazione** della proiezione e aggiornamento dei due lati delle associazioni con `festival.addProiezione`,
   `film.addProiezione`, `sala.addProiezione`; `impostaOrario` calcola `oraFine`; `save` esegue il persist.
6. **Commit** al ritorno del metodo; con qualsiasi eccezione **rollback** completo: non resta nulla di parziale.

**Perché serve il lock (accesso concorrente).** Senza lock, due amministratori che programmano nello stesso istante
due proiezioni sovrapposte nella stessa sala eseguirebbero entrambi il passo 4 prima che l'altro abbia fatto commit:
in READ_COMMITTED nessuno vede l'`INSERT` non confermato dell'altro, entrambi trovano la sala libera, entrambi
inseriscono. È un **write skew**: nessuna lettura "sporca", ma il risultato viola la regola.
Con il lock la seconda transazione si ferma al passo 3 finché la prima non termina; quando riparte, la sua query del
passo 4 (in READ_COMMITTED ogni istruzione vede i dati confermati in quel momento) trova la nuova proiezione e
lancia il conflitto. Il lock riguarda solo la sala: programmazioni in sale diverse procedono in parallelo.
**Alternativa**: `Isolation.SERIALIZABLE` senza lock. PostgreSQL rileva il conflitto e annulla una delle due
transazioni con un errore di serializzazione (SQLState `40001`); l'applicazione deve ripetere l'operazione (retry).

Test che lo dimostrano (`service/ProiezioneServiceIntegrationTest`):
- `programmazioneFallitaNonLasciaDatiParziali`: un conflitto non lascia righe nuove;
- `seLaTransazioneFallisceVieneAnnullataAncheLaParteGiaEseguita`: due programmazioni dentro la stessa transazione
  esterna (`TransactionTemplate`); la seconda fallisce e viene annullata anche la prima, già inserita;
- `dueProgrammazioniConcorrentiNellaStessaSala`: due thread partono insieme (`CountDownLatch`); ne riesce
  esattamente uno. Il `SecurityContext` viene passato ai thread con `DelegatingSecurityContextExecutorService`.
  Sostituendo `findByIdForUpdate` con `findById` il test fallisce: vengono inserite entrambe.

### `FestivalService.rimuoviFilm`, il secondo caso multi-entità
```java
festival.removeFilm(film);                                   // DELETE dalla join table al commit
for (Proiezione p : proiezioneRepository.findByFestivalIdAndFilmId(festivalId, filmId)) {
    if (p.getStato() == StatoProiezione.SCHEDULED) {
        p.setStato(StatoProiezione.CANCELLED);               // UPDATE al commit (dirty checking)
    }
}
```
Tutto nella stessa transazione: o si applicano sia la rimozione sia gli annullamenti, o nessuno dei due. Le proiezioni
COMPLETED restano (storico). Il metodo restituisce il numero di proiezioni annullate, mostrato nel messaggio flash.

### Recensioni e concorrenza
`RecensioneService.crea` fa prima `existsByFilmIdAndUtenteId` (messaggio chiaro nel caso normale), poi `saveAndFlush`:
il flush immediato fa partire subito l'`INSERT`; se due richieste dello stesso utente superano insieme il controllo,
il vincolo unique fa fallire la seconda e la `DataIntegrityViolationException` viene tradotta in
`RecensioneDuplicataException`. Il rollback avviene comunque, perché l'eccezione rilanciata è unchecked.

---

## 6. Sicurezza

### La catena di filtri
Ogni richiesta attraversa la `SecurityFilterChain` definita in `security/SecurityConfig.java` prima di arrivare ai
controller. I filtri principali, nell'ordine:
1. **SecurityContextHolderFilter**: recupera dalla sessione HTTP (cookie `JSESSIONID`) l'utente già autenticato e lo
   mette nel `SecurityContext` del thread.
2. **CsrfFilter**: per i metodi che modificano dati (POST, PUT, DELETE) confronta il token inviato (campo `_csrf` o
   header `X-XSRF-TOKEN`) con quello del cookie `XSRF-TOKEN`; se manca o non coincide → 403.
3. **LogoutFilter**: gestisce `POST /logout` (invalida la sessione, redirect a `/?logout`).
4. **UsernamePasswordAuthenticationFilter**: gestisce `POST /login` del form. Chiama
   `UtenteDetailsService.loadUserByUsername`, confronta con BCrypt la password inserita con l'hash salvato e, se
   corrispondono, salva l'utente nella sessione e fa redirect alla pagina richiesta o a `/`. Altrimenti
   `/login?error`.
5. **CsrfCookieFilter** (nostro): legge il token a ogni richiesta, così il cookie `XSRF-TOKEN` viene scritto anche
   per le pagine React (Spring Security 6 lo genera solo quando qualcuno lo usa).
6. **AnonymousAuthenticationFilter**: se nessuno è autenticato, mette un utente "anonimo".
7. **ExceptionTranslationFilter** + **AuthorizationFilter**: applicano le regole sugli URL. Se l'utente è anonimo e la
   risorsa è protetta chiama l'*entry point* (`ApiAuthenticationEntryPoint`: redirect al login per le pagine, 401 JSON
   per `/api/**`); se è autenticato ma senza permesso chiama l'*access denied handler* (`ApiAccessDeniedHandler`:
   pagina 403 per le pagine, 403 JSON per le API).

### Regole sugli URL (valutate in ordine, vince la prima)
```java
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.GET, "/api/**").permitAll()
.requestMatchers("/api/movies/*/reviews", "/api/reviews/**").hasAnyRole("USER", "ADMIN")
.requestMatchers("/api/**").hasRole("ADMIN")
.anyRequest().permitAll()
```

### Utenti, ruoli, password
- `UtenteDetailsService` carica l'`Utente` e costruisce uno `UserDetails` con `roles(ruolo)`, cioè l'autorità
  `ROLE_USER` o `ROLE_ADMIN`; `hasRole('ADMIN')` controlla `ROLE_ADMIN`.
- Le password sono salvate con **BCrypt** (`PasswordConfig`): hash lento e con *salt* casuale incluso nell'hash; la
  stessa password produce hash diversi, e non si può risalire alla password.
- La registrazione (`AuthController` → `UtenteService.registra`) crea sempre utenti `USER`; gli admin arrivano dal seed.

### Due livelli di autorizzazione
1. **URL** (sopra): proteggono intere aree.
2. **Metodi dei service** con `@PreAuthorize` (abilitato da `@EnableMethodSecurity`):
   - scritture di gestione: `@PreAuthorize("hasRole('ADMIN')")` (festival, film, registi, attori, sale, proiezioni);
   - recensioni: `hasAnyRole('USER','ADMIN') and #username == authentication.name` (costante `UTENTE_AUTENTICATO` in
     `RecensioneService`): oltre al ruolo, lo username passato al metodo deve essere quello dell'utente autenticato.

Anche se un giorno un nuovo controller dimenticasse la regola sull'URL, il service resterebbe protetto.

### Proprietà delle recensioni
La regola "si modificano e si eliminano solo le proprie recensioni" richiede di leggere l'autore dal DB, quindi è
nel codice del service (`RecensioneService.trovaPropria`):
```java
if (!recensione.getUtente().getUsername().equals(username)) {
    throw new OperazioneNonAutorizzataException("Puoi modificare o eliminare solo le tue recensioni");
}
```
→ 403 (pagina o ProblemDetail). React nasconde i pulsanti sulle recensioni altrui, ma la sicurezza non dipende da
questo: una `PUT /api/reviews/5` costruita a mano viene comunque rifiutata (test
`ApiRestTest.modificareLaRecensioneDiUnAltroRestituisce403`).

### CSRF con React
React gira sulla stessa origine e usa la stessa sessione: un sito malevolo potrebbe far partire dal browser
dell'utente una richiesta verso le nostre API, e il browser allegherebbe il cookie di sessione. Per questo il CSRF
resta attivo anche per le API:
- `CookieCsrfTokenRepository.withHttpOnlyFalse()` salva il token nel cookie `XSRF-TOKEN`, **leggibile da JavaScript**;
- `api.js` lo legge e lo mette nell'header `X-XSRF-TOKEN`;
- un sito esterno non può leggere i cookie del nostro dominio, quindi non può costruire l'header giusto.
- `CsrfTokenRequestAttributeHandler` confronta il token "in chiaro" (quello che React legge dal cookie); i form
  Thymeleaf includono il token come campo nascosto `_csrf` automaticamente grazie a `th:action`.

---

## 7. API REST

| Metodo | URL | Input | Output | Status |
|---|---|---|---|---|
| GET | `/api/festivals` | – | `FestivalDto[]` | 200 |
| GET | `/api/festivals/{id}` | – | `FestivalDto` | 200, 404 |
| GET | `/api/festivals/{id}/movies` | – | `FilmDto[]` (con regista) | 200, 404 |
| GET | `/api/festivals/{id}/screenings?date=AAAA-MM-GG` | `date` opzionale | `ProiezioneDto[]` (film, sala) | 200, 400 data malformata, 404 |
| GET | `/api/movies?title=&genre=&director=&page=0&size=10` | filtri opzionali | `PaginaDto<FilmDto>` | 200 |
| GET | `/api/movies/{id}` | – | `FilmDettaglioDto` | 200, 404 |
| GET | `/api/movies/{id}/reviews` | – | `RecensioniFilmDto` (media, numero, elenco) | 200, 404 |
| POST | `/api/movies/{id}/reviews` | `{testo, voto}` | `RecensioneDto` + header `Location` | 201, 400, 401, 403 (CSRF), 404, 409 |
| GET | `/api/reviews/{id}` | – | `RecensioneDto` | 200, 404 |
| PUT | `/api/reviews/{id}` | `{testo, voto}` | `RecensioneDto` | 200, 400, 401, 403, 404 |
| DELETE | `/api/reviews/{id}` | – | – | 204, 401, 403, 404 |
| GET | `/api/me` | – | `UtenteCorrenteDto` | 200 |

Note:
- I controller convertono in DTO le entità restituite dai service (metodi `da(...)`); tutto ciò che il DTO legge è
  già caricato. `ProiezioneDto` ha due conversioni, `perProgramma` e `perFilm`, perché i due casi d'uso caricano
  associazioni diverse; i campi `null` non vengono serializzati.
- L'autore delle recensioni è `authentication.getName()`, mai un campo del JSON.
- `201 Created` con `Location` (costruito con `ServletUriComponentsBuilder`), `204 No Content` per la DELETE.
- La documentazione interattiva è su `/swagger-ui.html` (springdoc, configurazione in `config/OpenApiConfig.java` e in
  `application.properties`).

### Gestione centralizzata degli errori
`exception/ApiExceptionHandler.java` è un `@RestControllerAdvice` per i controller di `controller.api`, con
precedenza massima. Estende `ResponseEntityExceptionHandler`, che converte già in **ProblemDetail** (RFC 7807) le
eccezioni standard di Spring MVC (JSON malformato, parametro del tipo sbagliato, metodo non supportato...).
Aggiunge:

| Eccezione | Status | `title` |
|---|---|---|
| `MethodArgumentNotValidException` (`@Valid` fallito) | 400 | Dati non validi, con proprietà `errori: [{campo, messaggio}]` |
| `RegolaBusinessException` | 400 | Regola di business violata |
| `OperazioneNonAutorizzataException` | 403 | Operazione non consentita |
| `EntityNotFoundException` | 404 | Risorsa non trovata |
| `RecensioneDuplicataException`, `ConflittoProgrammazioneException`, `DataIntegrityViolationException` | 409 | Conflitto |

Esempio di risposta:
```json
{ "type": "about:blank", "title": "Dati non validi", "status": 400, "detail": "Alcuni campi non sono validi",
  "instance": "/api/movies/1/reviews", "errori": [ { "campo": "voto", "messaggio": "Il voto va da 1 a 5" } ] }
```
401 e 403 dovuti alla sicurezza non passano da qui: li producono i filtri (`ApiAuthenticationEntryPoint`,
`ApiAccessDeniedHandler`), sempre in formato ProblemDetail.

---

## 8. Frontend

### Thymeleaf
- **Layout**: `templates/fragments/layout.html` contiene i frammenti `head(titolo)`, `navbar`, `flash`,
  `campo(nome, etichetta, tipo)`, `erroriGlobali`, `footer`. Ogni pagina li include con
  `th:replace="~{fragments/layout :: navbar}"`.
- **Navbar per ruolo** con il dialetto `sec:` (`thymeleaf-extras-springsecurity6`): `sec:authorize="hasRole('ADMIN')"`
  mostra il menu Gestione, `sec:authentication="name"` stampa lo username.
- **Form**: `th:object="${form}"` lega il form all'oggetto; `th:field="*{nome}"` genera `name`, `id` e valore;
  `th:errors` mostra gli errori di validazione accanto al campo; `th:action` aggiunge il token CSRF.
- **Pagine pubbliche**: `index.html`, `festivals/list.html` (con il frammento `cards` riusato dalla home),
  `festivals/detail.html`, `festivals/films.html`, `auth/login.html`, `auth/register.html`.
- **Area admin**: `admin/dashboard.html`, `admin/festivals/{list,form,films}.html`, `admin/films/{list,form,cast}.html`,
  `admin/persone/{list,form}.html` (registi e attori, parametrizzati con `base`), `admin/sale/{list,form}.html`,
  `admin/proiezioni/{list,form}.html`.
- **Errori**: `error/403.html`, `error/404.html`, `error/error.html` (usati sia da `MvcExceptionHandler` sia dalla
  gestione errori di Spring Boot).
- I link verso le pagine React sono normali `<a th:href="@{/app/movies/{id}(id=${film.id})}">`.

### React (`frontend/src`)
| File | Ruolo |
|---|---|
| `main.jsx` | monta `<App>` dentro `<BrowserRouter basename="/app">` |
| `App.jsx` | carica l'utente corrente e definisce le rotte `/movies/:id` e `/festivals/:id/screenings` |
| `api.js` | wrapper di `fetch`: cookie di sessione, header `X-XSRF-TOKEN`, errori come `ApiError` (status + ProblemDetail), `messaggioErrore` |
| `useCurrentUser.js` | hook che chiama `GET /api/me` al primo render |
| `formato.js` | formattazione di date e orari ISO (senza problemi di fuso orario) |
| `components/Navbar.jsx` | stessa navbar delle pagine Thymeleaf; logout con form POST e `_csrf` |
| `pages/MovieDetailPage.jsx` | legge `id` dall'URL, carica `GET /api/movies/{id}`, gestisce caricamento ed errore |
| `components/MovieInfo.jsx` | dati del film, regista, cast, festival, proiezioni |
| `components/ReviewsSection.jsx` | elenco e media (`GET /api/movies/{id}/reviews`), crea/modifica/elimina, ricarica dopo ogni scrittura |
| `components/ReviewForm.jsx` | form controllato; 400 → errori sui campi, 401/403/409 → messaggio |
| `components/ReviewItem.jsx` | una recensione; Modifica (form in linea) ed Elimina se è dell'utente |
| `components/Stelle.jsx` | voto come stelle |
| `pages/ScreeningsPage.jsx` | festival e programma; il giorno scelto sta nell'URL (`?date=`) e viene passato all'API |
| `components/DateFilter.jsx` | select con i giorni del festival |
| `components/ScreeningsTable.jsx` | proiezioni raggruppate per giorno, link al dettaglio film |

**Stato ed effetti.** Ogni pagina tiene i dati in `useState` (`film`, `caricamento`, `errore`) e li carica in un
`useEffect` con le dipendenze giuste: `[id]` in `MovieDetailPage` (ricarica se cambia il film), `[id, data]` in
`ScreeningsPage` (ricarica se cambia il filtro). In `ReviewsSection` la funzione `carica` è avvolta in
`useCallback` per poterla usare come dipendenza dell'effetto.

**Errori.** `api.js` lancia `ApiError` per ogni risposta non 2xx. `ReviewForm` distingue: 400 con `errori` → messaggi
sotto i campi; altrimenti `messaggioErrore` (401 "Devi accedere...", 403, 404, 409 con il `detail` del backend).

### Come la build di React entra in Spring Boot
1. `pom.xml` contiene `frontend-maven-plugin`: nella fase `generate-resources` scarica Node 22 in `frontend/node`,
   esegue `npm install` e `npm run build` dentro `frontend/`.
2. `frontend/vite.config.js` ha `base: '/app/'` e `build.outDir: '../src/main/resources/static/app'`: i file compilati
   (`index.html`, `assets/index-<hash>.js`) finiscono tra le risorse statiche.
3. Maven copia le risorse e le mette nel jar; Spring Boot serve tutto ciò che sta in `static/` (quindi `/app/index.html`,
   `/app/assets/...`).
4. Se l'utente apre direttamente `/app/movies/3`, sul server non esiste quel file: `ReactForwardController` fa un
   *forward* interno a `/app/index.html`, poi il router di React legge l'URL e mostra la pagina giusta.
5. Stessa origine → stessa sessione: React vede l'utente che ha fatto login nelle pagine Thymeleaf.

---

## 9. Validazione ed errori

### Dove avviene la validazione
1. **Form e corpi JSON** (`dto/form`): annotazioni Bean Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`,
   `@Past`, `@Pattern`). Il controller le attiva con `@Valid`:
   - Thymeleaf: `@Valid @ModelAttribute("form") FestivalForm form, BindingResult bindingResult` → se
     `bindingResult.hasErrors()` si rimostra il form con i messaggi;
   - REST: `@Valid @RequestBody RecensioneForm form` → se non valido `MethodArgumentNotValidException` → 400.
2. **Vincolo di classe** `@PeriodoValido` su `FestivalForm`: confronta due campi (data fine ≥ data inizio);
   `PeriodoValidoValidator` associa l'errore al campo `dataFine`.
3. **Regole di business nel service** (dipendono dal DB o da più entità): sovrapposizioni, periodo del festival,
   partecipazione del film, passato, mezzanotte, recensione duplicata, proprietà. Sono ripetute nel service anche
   quando il form le controlla già (es. il periodo), perché il service può essere chiamato da altri punti.
4. **Entità**: le stesse annotazioni sulle entità vengono verificate da Hibernate prima di scrivere.
5. **Database**: `NOT NULL`, `CHECK`, `UNIQUE` (§3), ultima garanzia anche in caso di concorrenza.

### Le eccezioni di dominio e come arrivano all'utente
| Eccezione | Quando | Pagina Thymeleaf | API |
|---|---|---|---|
| `EntityNotFoundException` | id inesistente | `error/404` (404) | 404 ProblemDetail |
| `RegolaBusinessException` | regola violata | errore nel form (catturata dal controller) o `error/error` (400) | 400 |
| `ConflittoProgrammazioneException` | sala occupata | errore nel form di programmazione | 409 |
| `RecensioneDuplicataException` | seconda recensione | – | 409 |
| `OperazioneNonAutorizzataException` | non sei l'autore | `error/403` | 403 |
| `UsernameGiaUsatoException` | registrazione | errore sul campo username | – |

Nei form admin le regole violate vengono catturate nel controller e aggiunte al `BindingResult` con
`bindingResult.reject("regola", e.getMessage())`: compaiono sopra il form (frammento `erroriGlobali`) e l'utente non
perde i dati inseriti. Per le azioni senza form (rimuovi film, annulla proiezione) il messaggio arriva come flash
`errore` dopo il redirect. Tutto ciò che non è gestito nel controller passa da `MvcExceptionHandler`
(`@ControllerAdvice` con precedenza minima) o, per le API, da `ApiExceptionHandler`.

---

## 10. Esperimento sulle strategie di fetch

### Come si lancia
```bash
./mvnw test -Dtest=FetchStrategyExperimentTest
```
(aggiungendo `-Dskip.npm -Dskip.installnodenpm` si salta la build di React). Il test è
`experiment/FetchStrategyExperimentTest.java`, profilo `experiment` (`src/test/resources/application-experiment.properties`):
database `festival_test`, `hibernate.generate_statistics=true`, log SQL formattato.

### Cosa misura
Dataset generato dal test (`generaDataset`): 1 festival, 100 film con 100 registi diversi, 4 sale, 300 proiezioni.
- **Caso 1 – film del festival con i registi** (`filmDelFestivalConRegisti`): LAZY, JOIN FETCH, @EntityGraph, batch
  fetch (25). Il "caso d'uso" è `accediAiRegisti`: per ogni film legge il cognome del regista.
- **Caso 2 – programma con film e sale** (`programmaDelFestivalConFilmESale`): LAZY e JOIN FETCH.

Il metodo `misura` esegue la strategia in una **nuova transazione** (`TransactionTemplate`), con
`entityManager.clear()` e `statistics.clear()` all'inizio, misura il tempo con `System.nanoTime()` e legge
`statistics.getPrepareStatementCount()` (query) e `getEntityLoadCount()` (entità caricate). Un giro di
`riscaldamento` non conteggiato precede le misure (JIT, connessioni del pool, piani di query).

### Come leggere l'output e il log
```
Strategia 1: LAZY
Film caricati: 100
Query SQL: 101
Oggetti caricati in totale: 200
Tempo: 34 ms
```
Sopra ogni risultato il log (`org.hibernate.SQL`) mostra le query:
- LAZY: una `select ... from film join festival_film ...` seguita da 100 `select ... from regista where r1_0.id=?`;
- JOIN FETCH e @EntityGraph: una sola `select f1_0..., r1_0... from film f1_0 join festival_film ... join regista r1_0 ...`;
- batch: la query dei film e 4 `select ... from regista r1_0 where r1_0.id = any (?)`.

### Perché i numeri escono così
| Strategia | Query | Spiegazione |
|---|---|---|
| LAZY | 101 = 1 + 100 | un proxy per film, inizializzato al primo accesso |
| JOIN FETCH | 1 | registi nella stessa SELECT |
| @EntityGraph | 1 | come sopra, join aggiunta dal grafo |
| Batch (25) | 5 = 1 + 100/25 | proxy inizializzati a gruppi di 25 |
| Programma LAZY | 105 = 1 + 100 + 4 | film e sale **distinti**: ogni entità si carica una volta per persistence context |
| Programma JOIN FETCH | 1 | due to-one in join, nessun prodotto cartesiano |

Gli oggetti caricati sono gli stessi in tutte le strategie (200 nel caso 1, 404 nel caso 2): cambia solo quante
volte si va al database. I tempi includono la scrittura del log: contano gli ordini di grandezza, non i millisecondi.

### Con 10, 1.000 o 100.000 film
| Film | LAZY | JOIN FETCH / EntityGraph | Batch 25 |
|---|---|---|---|
| 10 | 11 | 1 | 2 |
| 1.000 | 1.001 | 1 | 41 |
| 100.000 | 100.001 | 1 | 4.001 |

Con LAZY il tempo cresce linearmente ed è dominato dalla latenza di ogni andata e ritorno (qui il DB è locale; in
produzione, in rete, ogni query costa di più). Con JOIN FETCH resta una query, crescono solo le righe trasferite;
con 100.000 film servirebbe comunque la paginazione (come `GET /api/movies`). Le asserzioni del test fissano
101/1/1/5 e 105/1: se una modifica reintroduce un N+1, il test fallisce.

---

## 11. Come modificare il progetto

Schema generale: **modello → repository → service → controller/DTO → vista/React → test**. Dopo ogni modifica:
`./mvnw verify -Dskip.npm -Dskip.installnodenpm` (oppure solo i test interessati con `-Dtest=NomeTest`).
Con `ddl-auto=update` le nuove colonne e tabelle vengono create all'avvio (le colonne non vengono mai rimosse).

### Aggiungere un attributo (es. `lingua` del film)
1. `model/Film.java`: campo `@NotBlank private String lingua;` + getter/setter (+ parametro nel costruttore se serve).
2. `dto/form/FilmForm.java`: stesso campo con validazione; aggiornare `FilmForm.da(film)`.
3. `service/FilmService.copiaDati`: `film.setLingua(form.getLingua());`.
4. `templates/admin/films/form.html`: `<div th:replace="~{fragments/layout :: campo('lingua', 'Lingua', 'text')}"></div>`.
5. Visualizzazione: `templates/admin/films/list.html`; per React `dto/api/FilmDettaglioDto` (campo + `da`) e
   `frontend/src/components/MovieInfo.jsx`.
6. Dati esistenti: con `@NotBlank` la nuova colonna sarebbe `NOT NULL` e l'`ALTER TABLE` fallirebbe su una tabella
   già piena; si può renderla facoltativa, oppure svuotare e ricreare il DB di sviluppo (il seed la valorizza).

### Aggiungere una relazione (es. un festival ha un direttore artistico, un `Regista`)
1. `model/Festival.java`: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "direttore_id") private Regista direttore;`
   (lato proprietario: la FK sta in `festival`). Se serve il lato inverso: in `Regista`
   `@OneToMany(mappedBy = "direttore") private List<Festival> festivalDiretti`.
2. Caricamento: dove serve mostrarlo, query con `left join fetch fe.direttore` in `FestivalRepository`.
3. `FestivalForm` + `direttoreId`, `FestivalService.copiaDati` (carica il regista con `RegistaRepository.findById`),
   select nel form admin come quella del regista in `admin/films/form.html`.
Per una molti-a-molti (es. giuria: festival ↔ attori): `@ManyToMany @JoinTable(name = "festival_giuria", ...) Set<Attore>`
in `Festival` (proprietario) e `@ManyToMany(mappedBy = "giuria")` in `Attore`, con metodi helper.

### Aggiungere una query (es. proiezioni di una sala in un giorno)
```java
// ProiezioneRepository
@Query("select p from Proiezione p join fetch p.film where p.sala.id = :salaId and p.data = :data order by p.ora")
List<Proiezione> findBySalaEGiorno(Long salaId, LocalDate data);
```
Poi un metodo `@Transactional(readOnly = true)` nel service che la usa. Ricordare il `join fetch` per tutto ciò che
la vista mostrerà; le query derivate dal nome vanno bene per i casi semplici (`findBySalaIdAndData`).

### Aggiungere un caso d'uso (es. i film meglio votati)
1. Repository: `@Query("select r.film.id, avg(r.voto) from Recensione r group by r.film.id order by avg(r.voto) desc")`
   oppure un record di proiezione (`select new ...`); poi caricare i film con il regista in una seconda query.
2. Service: `@Transactional(readOnly = true) public List<...> classifica(int quanti)`.
3. Controller: una pagina Thymeleaf (`@GetMapping("/classifica")` + template) o un endpoint REST.
4. Test: un test di integrazione con qualche recensione.

### Aggiungere un endpoint REST (es. `GET /api/sale`)
1. DTO: `public record SalaDto(Long id, String nome, String indirizzo, Integer capienza) { static SalaDto da(Sala s) {...} }`.
2. Controller in `controller/api` (o metodo in uno esistente):
   `@GetMapping("/api/sale") public ResponseEntity<List<SalaDto>> sale() { return ResponseEntity.ok(salaService.elenco().stream().map(SalaDto::da).toList()); }`
3. Sicurezza: i GET su `/api/**` sono già pubblici; un POST sarebbe riservato all'ADMIN dalla regola `/api/**`.
4. Errori: le eccezioni di dominio vengono già tradotte da `ApiExceptionHandler`.
5. Test in `controller/api/ApiRestTest`.

### Cambiare una regola di business (es. 15 minuti di pausa tra due proiezioni nella stessa sala)
Tutto in `service/ProiezioneService.verificaDisponibilita`: passare alla query `ora.minusMinutes(15)` e
`oraFine.plusMinutes(15)` (attenzione all'attraversamento della mezzanotte). Il controller non cambia. Aggiornare
`ProiezioneServiceTest` e i test di repository con un caso "a 10 minuti" (conflitto) e uno "a 15 minuti" (ok).

### Cambiare la gestione transazionale (es. SERIALIZABLE al posto del lock)
1. In `programmaProiezione`: `@Transactional(isolation = Isolation.SERIALIZABLE)` e `salaRepository.findById` al posto
   di `findByIdForUpdate`.
2. In caso di conflitto PostgreSQL annulla una transazione con un errore di serializzazione (SQLState `40001`, che
   Spring traduce in un'eccezione della famiglia `ConcurrencyFailureException`): va gestito ripetendo l'operazione (un ciclo di
   tentativi **fuori** dal metodo transazionale, ad esempio in un altro bean, perché ogni tentativo deve essere una
   nuova transazione) oppure restituendo un 409.
3. Rilanciare `ProiezioneServiceIntegrationTest.dueProgrammazioniConcorrentiNellaStessaSala`.
Per vedere l'effetto del lock basta sostituire `findByIdForUpdate` con `findById`: il test concorrente fallisce.

### Cambiare la sicurezza (es. l'ADMIN può eliminare qualunque recensione)
1. Regola sugli URL: già consentita (`hasAnyRole('USER','ADMIN')`).
2. Service: in `RecensioneService.elimina`, saltare il controllo di proprietà se l'utente è admin, ad esempio con un
   parametro `boolean admin` calcolato nel controller da `authentication.getAuthorities()`, oppure con
   `@PreAuthorize("hasRole('ADMIN') or #username == authentication.name")` più un controllo nel codice.
3. React: mostrare il pulsante Elimina anche quando `utente.ruolo === 'ADMIN'` (`ReviewsSection.jsx`/`ReviewItem.jsx`).
4. Test: un caso in `ApiRestTest` con `@WithMockUser(roles = "ADMIN")`.
Per proteggere un nuovo URL: aggiungere un `requestMatchers(...)` in `SecurityConfig` **prima** di `anyRequest()`.

### Cambiare la strategia di fetch (es. regista EAGER)
`Film.regista`: `@ManyToOne(fetch = FetchType.EAGER, ...)`. EAGER non elimina l'N+1: con una query JPQL senza
`join fetch` (come `findByFestivalId`) Hibernate esegue la query dei film e **subito dopo** una query per ogni regista,
anche se il regista non verrà mai usato. Nell'esperimento la strategia "LAZY" continua a fare 101 query, ma ora le fa
durante la query invece che al primo accesso; JOIN FETCH ed EntityGraph restano a 1. In più ogni altro caso d'uso che
carica film (anche dove il regista non serve) paga il costo dei registi. Per osservarlo: cambiare il mapping, rilanciare
l'esperimento e guardare nel log *quando* partono le query. Per tornare indietro, rimettere LAZY.

### Individuare e correggere un N+1
Sintomo: nel log (`logging.level.org.hibernate.SQL=DEBUG`) compaiono tante query uguali che cambiano solo per l'id.
Esempio: nella lista admin dei film si mostra il regista; se `FilmService.elenco` usasse `findAll()`, la pagina
farebbe 1 + N query (con `open-in-view=false` darebbe addirittura `LazyInitializationException`).
Correzione (già presente): `FilmRepository.findAllWithRegista` con `join fetch f.regista`. In alternativa
`@EntityGraph(attributePaths = "regista")` su un metodo del repository, o `@BatchSize(size = 25)` sull'entità `Regista`.
Verifica: contare le query con le statistiche (come nell'esperimento) o leggere il log.

### Modificare un componente React (es. mostrare la durata nel programma)
1. Il dato deve arrivare dall'API: `ProiezioneDto.perProgramma` espone il film come `Riferimento(id, nome)`; aggiungere
   la durata (es. un campo `durataFilm` nel DTO, valorizzato con `p.getFilm().getDurata()`: il film è già caricato).
2. `frontend/src/components/ScreeningsTable.jsx`: nuova colonna `<th>Durata</th>` e `<td>{p.durataFilm} min</td>`.
3. Rivedere in sviluppo con `npm run dev` (ricaricamento automatico) oppure ricompilare con `./mvnw package`.

---

## 12. Glossario

- **Entità**: classe Java annotata `@Entity`, mappata su una tabella.
- **EntityManager / Session**: l'oggetto JPA (Hibernate) con cui si leggono e scrivono entità; Spring Data lo usa dietro i repository.
- **Persistence context**: la "memoria" dell'EntityManager durante una transazione; contiene al massimo un oggetto per ogni
  riga (stesso id → stesso oggetto). È la cache di primo livello.
- **Stati di un'entità**: *transient* (nuova, sconosciuta a Hibernate), *managed* (nel persistence context: le modifiche
  vengono salvate), *detached* (era managed, la transazione è finita), *removed*.
- **Dirty checking**: al flush Hibernate confronta le entità managed con lo stato letto e genera gli `UPDATE` necessari,
  senza bisogno di chiamare `save`.
- **Flush**: invio al DB delle modifiche accumulate (avviene prima del commit, prima di certe query, o con `saveAndFlush`).
- **Proxy Hibernate**: sottoclasse generata a runtime che sostituisce un'entità non ancora caricata; si inizializza
  al primo accesso a un campo (non all'accesso all'id).
- **Lazy / Eager loading**: caricamento al primo accesso / caricamento immediato insieme all'entità che contiene l'associazione.
- **LazyInitializationException**: accesso a un proxy o a una collezione LAZY quando la sessione è chiusa.
- **open-in-view**: impostazione di Spring Boot che tiene aperta la sessione per tutta la richiesta; qui è disattivata.
- **Problema N+1**: 1 query per una lista + 1 query per ogni elemento per caricare un'associazione.
- **JOIN FETCH**: join JPQL che carica l'associazione nella stessa query.
- **EntityGraph**: dichiarazione degli attributi da caricare insieme all'entità, applicata a una query.
- **Batch fetching**: inizializzazione di più proxy dello stesso tipo con una sola query (`id in (...)`).
- **MultipleBagFetchException**: errore di Hibernate quando si fa fetch di più `List` (bag) nella stessa query.
- **Prodotto cartesiano**: moltiplicazione delle righe quando si fa join di più collezioni.
- **Lato proprietario / `mappedBy`**: in un'associazione bidirezionale il lato proprietario è quello che scrive la FK o
  la join table; l'altro lato dichiara `mappedBy` e non scrive nulla.
- **Cascade**: propagazione di un'operazione (persist, remove...) dall'entità alle associate.
- **orphanRemoval**: cancellazione automatica di un elemento tolto da una collezione `@OneToMany`.
- **Transazione**: gruppo di operazioni atomico (tutto o niente), con commit o rollback.
- **Propagazione**: cosa fa un metodo transazionale se una transazione esiste già (`REQUIRED`: vi partecipa).
- **Rollback-only**: stato di una transazione in cui un errore ha già deciso che finirà con un rollback.
- **Livello di isolamento**: quanto una transazione vede le modifiche delle altre (READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE).
- **Write skew**: anomalia in cui due transazioni leggono gli stessi dati, decidono che un'operazione è lecita e scrivono
  righe diverse, producendo insieme uno stato non valido.
- **Lock pessimistico**: blocco di una riga (`SELECT ... FOR UPDATE`) fino alla fine della transazione.
- **Proxy di Spring (AOP)**: oggetto che avvolge un bean e aggiunge comportamento (transazioni, sicurezza) attorno ai metodi.
- **DTO**: oggetto che trasporta dati verso l'esterno (qui: JSON), separato dalle entità.
- **Form object**: oggetto legato a un form HTML, con le regole di validazione.
- **Bean Validation**: standard (`jakarta.validation`) per esprimere vincoli con annotazioni.
- **ProblemDetail**: formato JSON standard (RFC 7807) per descrivere un errore HTTP.
- **Post/Redirect/Get**: dopo un POST riuscito si risponde con un redirect, così ricaricare la pagina non reinvia il form.
- **Messaggio flash**: attributo che sopravvive a un solo redirect (`RedirectAttributes.addFlashAttribute`).
- **SecurityFilterChain**: catena di filtri di Spring Security attraversata da ogni richiesta.
- **SecurityContext**: contiene l'utente autenticato della richiesta corrente.
- **Autenticazione / autorizzazione**: chi sei / cosa puoi fare.
- **BCrypt**: algoritmo di hash lento e con salt per le password.
- **CSRF**: attacco in cui un sito esterno fa partire richieste verso il nostro sito usando la sessione dell'utente;
  si previene con un token che il sito esterno non può conoscere.
- **SPA / client-side routing**: applicazione React che cambia pagina senza ricaricare, leggendo l'URL nel browser.
- **Hook React**: funzioni come `useState` (stato), `useEffect` (effetti dopo il render), `useCallback` (funzione stabile).
