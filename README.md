# Festival cinematografici – Sistemi Informativi su Web

Progetto assegnato dal docente (SIW, Università Roma Tre, appello di novembre 2026): un sistema informativo
per la gestione di festival cinematografici, con film, cast, sale, proiezioni e recensioni.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Data JPA / Hibernate 6 · PostgreSQL · Spring Security ·
Thymeleaf + Bootstrap 5 · React 18 (Vite) · springdoc-openapi (Swagger).

La spiegazione completa del codice, scritta per poterlo discutere e modificare, è in
[GUIDA_AL_CODICE.md](GUIDA_AL_CODICE.md).

## Funzionalità
**Pubbliche (senza login)**
- elenco e dettaglio dei festival, film partecipanti a un festival (Thymeleaf);
- programma delle proiezioni di un festival, raggruppato per giorno e filtrabile per data (React);
- dettaglio di un film con regista, cast, festival, proiezioni e recensioni con media dei voti (React).

**Utente registrato (`USER`)**
- registrazione e login;
- inserimento di una recensione (una sola per film), modifica ed eliminazione delle **proprie** recensioni.

**Amministratore (`ADMIN`)**
- creazione e modifica di festival, film (con regista e cast), registi, attori e sale;
- associazione e rimozione di film da un festival (le proiezioni programmate del film vengono annullate);
- programmazione, modifica, annullamento delle proiezioni, con controlli di consistenza: nessuna sovrapposizione
  nella stessa sala, data nel periodo del festival, film associato al festival, niente proiezioni nel passato.

## Prerequisiti
- JDK 21 o superiore. Con JDK più recenti di Java 21 il `pom.xml` abilita
  `-Dnet.bytebuddy.experimental=true`, necessario a Hibernate e Mockito per creare proxy e mock.
- PostgreSQL in locale sulla porta 5432, utente `postgres`, password `postgres`, con due database
  (il primo per l'applicazione, il secondo per i test e l'esperimento):
  ```sql
  CREATE DATABASE festival;
  CREATE DATABASE festival_test;
  ```
- Maven non serve: si usa il wrapper `mvnw` (`./mvnw` in Git Bash/Linux/macOS, `.\mvnw` in PowerShell o cmd).
  Node.js non serve: la build lo scarica in `frontend/node`.
- Nota per Windows: `mvnw.cmd` è stato corretto per funzionare anche se la cartella del progetto contiene
  parentesi quadre (la versione originale del wrapper leggeva i file con `Get-Content` senza `-LiteralPath`).

## Avvio
```bash
./mvnw spring-boot:run
```

Al primo avvio (profilo `dev`, attivo per default) Hibernate crea le tabelle e `DataInitializer` inserisce i dati
di esempio: 3 festival (Venezia già concluso, Roma e Cannes futuri, con date relative al giorno di avvio),
20 film reali con registi e cast, 5 sale, 44 proiezioni, alcune recensioni.

In alternativa si può creare il jar unico (backend + frontend React) e avviarlo:

```bash
./mvnw package -DskipTests
```

```bash
java -Dnet.bytebuddy.experimental=true -jar target/festival-0.0.1-SNAPSHOT.jar
```

### Indirizzi utili
| Pagina | URL |
|---|---|
| Home | http://localhost:8080/ |
| Festival (Thymeleaf) | http://localhost:8080/festivals |
| Programma di un festival (React) | http://localhost:8080/app/festivals/2/screenings |
| Dettaglio film con recensioni (React) | http://localhost:8080/app/movies/1 |
| Area amministrativa | http://localhost:8080/admin |
| Documentazione API (Swagger UI) | http://localhost:8080/swagger-ui.html |

### Credenziali di esempio
| Username | Password | Ruolo |
|---|---|---|
| admin | admin | ADMIN |
| user1 | user1 | USER |
| user2 | user2 | USER |

### Sviluppo del frontend
Con il backend avviato su 8080, il frontend può girare anche con il server di sviluppo di Vite (ricaricamento
automatico), che inoltra `/api`, `/login`, `/logout` e `/css` a Spring Boot:

```bash
cd frontend && npm run dev
```

(richiede Node installato, oppure si usa quello scaricato dalla build in `frontend/node`). Le pagine si aprono su
http://localhost:5173/app/... . Per l'uso normale non serve: `./mvnw package` o `spring-boot:run` servono già la
versione compilata di React.

## Test
```bash
./mvnw verify
```

93 test, tutti sullo stesso PostgreSQL locale (database `festival_test`, ricreato a ogni esecuzione):
- **unit test** dei service con Mockito (regole di programmazione, recensioni, festival);
- **integrazione** dei service: rollback di una programmazione fallita, due programmazioni concorrenti nella stessa
  sala (il lock pessimistico ne fa riuscire una sola), rimozione di un film da un festival, proprietà delle recensioni;
- **repository**: query di sovrapposizione, vincolo unique delle recensioni, JOIN FETCH ed EntityGraph;
- **MockMvc**: sicurezza (redirect al login, 401/403 JSON, CSRF, login, registrazione), tutte le pagine Thymeleaf,
  tutte le API REST;
- **esperimento sulle strategie di fetch** (vedi sotto), con asserzioni sul numero di query.

Per test più rapidi senza ricompilare il frontend: `./mvnw verify -Dskip.npm -Dskip.installnodenpm`.

## Esperimento sulle strategie di fetch
Il test `FetchStrategyExperimentTest` (profilo `experiment`, database `festival_test`) confronta strategie
diverse per lo stesso caso d'uso, sullo stesso insieme di dati: 1 festival, 100 film con 100 registi tutti
diversi, 4 sale, 300 proiezioni. Si lancia senza modificare il codice:

```bash
./mvnw test -Dtest=FetchStrategyExperimentTest
```

Per saltare la build del frontend React (più veloce):

```bash
./mvnw test -Dtest=FetchStrategyExperimentTest -Dskip.npm -Dskip.installnodenpm
```

Per ogni strategia l'esperimento apre una nuova transazione con il persistence context vuoto
(`entityManager.clear()`), azzera le `Statistics` di Hibernate, misura il tempo con `System.nanoTime()` e stampa
query eseguite (`getPrepareStatementCount()`) e oggetti caricati. Un giro di riscaldamento iniziale non viene
conteggiato. Nel profilo sono attivi `hibernate.generate_statistics` e il log SQL formattato
(`org.hibernate.SQL=DEBUG`), così sopra ogni risultato si vedono le query eseguite.

Output tipico (i tempi variano da macchina a macchina; quelli sotto vengono da un'esecuzione reale):

```
=== Test accesso ai film del festival ===

Strategia 1: LAZY
Film caricati: 100
Query SQL: 101
Oggetti caricati in totale: 200
Tempo: 34 ms

Strategia 2: JOIN FETCH
Film caricati: 100
Query SQL: 1
Oggetti caricati in totale: 200
Tempo: 4 ms

Strategia 3: @EntityGraph
Film caricati: 100
Query SQL: 1
Oggetti caricati in totale: 200
Tempo: 3 ms

Strategia 4: Batch fetch (25)
Film caricati: 100
Query SQL: 5
Oggetti caricati in totale: 200
Tempo: 9 ms

=== Test programma del festival (proiezioni con film e sale) ===

Strategia 1: LAZY
Proiezioni caricate: 300
Query SQL: 105
Oggetti caricati in totale: 404
Tempo: 43 ms

Strategia 2: JOIN FETCH
Proiezioni caricate: 300
Query SQL: 1
Oggetti caricati in totale: 404
Tempo: 6 ms
```

### Come leggere i risultati
| Strategia | Query (N film) | Cosa succede |
|---|---|---|
| LAZY | 1 + N | una `select` sui film; `film.getRegista()` è un proxy e al primo accesso a un suo campo parte una `select ... from regista where id = ?` per ogni film: è il problema **N+1** |
| JOIN FETCH | 1 | una sola `select ... from film join regista ...`: i registi arrivano con i film |
| @EntityGraph | 1 | stessa query della strategia LAZY, ma il grafo (`attributePaths = "regista"`) fa aggiungere a Hibernate la join: SQL equivalente al JOIN FETCH |
| Batch fetch (25) | 1 + ⌈N/25⌉ | la query resta LAZY, ma Hibernate carica i proxy dei registi a gruppi: `select ... from regista where id = any (?)` con 25 id in un unico parametro array di PostgreSQL |

Nel secondo caso la strategia LAZY esegue 1 query per le proiezioni + 1 per ogni **film distinto** (100) + 1 per
ogni **sala distinta** (4) = 105: Hibernate carica ogni entità una sola volta per persistence context, quindi le
300 proiezioni non producono 600 query. Il JOIN FETCH su due associazioni *to-one* resta una sola query senza
prodotto cartesiano (ogni proiezione ha un solo film e una sola sala).
Il numero di oggetti caricati è uguale in tutte le strategie: cambia solo *quante volte* si va al database.

### Come scala al crescere di N
| Film | LAZY | JOIN FETCH / @EntityGraph | Batch (25) |
|---|---|---|---|
| 10 | 11 query | 1 query | 1 + 1 = 2 query |
| 100 | 101 | 1 | 1 + 4 = 5 |
| 1.000 | 1.001 | 1 | 1 + 40 = 41 |
| 100.000 | 100.001 | 1 | 1 + 4.000 = 4.001 |

Con LAZY il tempo cresce linearmente con N, ed è dominato dalla latenza di rete di ogni andata e ritorno
verso il database (qui il DB è locale, in produzione ogni query costa molto di più). Con JOIN FETCH le query
restano una sola: cresce solo la quantità di righe trasferite. Con 100.000 film però anche una sola query
restituirebbe troppi dati per una pagina: lì serve la paginazione (come in `GET /api/movies`), che con JOIN FETCH
su associazioni *to-one* funziona senza problemi. Il JOIN FETCH di una *collezione* invece moltiplica le righe e
non si combina bene con la paginazione (Hibernate pagina in memoria): per questo il progetto non fa mai JOIN FETCH
di più collezioni insieme e, dove serve, usa query separate.

Le asserzioni del test fissano questi numeri (101, 1, 1, 5, 105, 1): se una modifica reintroduce un N+1, il test fallisce.

## Architettura
Architettura a livelli: ogni livello usa solo quello sottostante.

```
Browser ──► Spring Security (filtri: sessione, CSRF, ruoli)
              │
              ├─► controller        Thymeleaf (MVC): riceve la richiesta, valida il form, chiama il service, sceglie la vista
              ├─► controller.api    REST per React: riceve JSON, chiama il service, restituisce DTO
              │
              ▼
           service                  casi d'uso e regole di business, @Transactional, @PreAuthorize
              │
              ▼
           repository               Spring Data JPA: query con JOIN FETCH / @EntityGraph, lock pessimistico
              │
              ▼
           model (entità JPA) ──► PostgreSQL
```

| Package (`it.uniroma3.siw.festival`) | Contenuto |
|---|---|
| `model` | 9 entità JPA (Festival, Film, Regista, Attore, Interpretazione, Sala, Proiezione, Recensione, Utente) e 2 enum |
| `repository` | interfacce Spring Data JPA con le query di ogni caso d'uso |
| `service` | casi d'uso, regole di business, transazioni |
| `controller`, `controller.admin` | controller Thymeleaf (pagine pubbliche, login, area amministrativa) |
| `controller.api` | controller REST usati da React |
| `dto.form`, `dto.api` | form object con Bean Validation e DTO JSON (le entità non escono mai in JSON) |
| `exception` | eccezioni di dominio e gestori centralizzati (pagine HTML / ProblemDetail JSON) |
| `security` | configurazione Spring Security, `UserDetailsService`, risposte 401/403 JSON, filtro CSRF |
| `config` | dati di esempio e metadati OpenAPI |

Scelte principali:
- **Fetch**: tutte le associazioni sono LAZY (anche le `@ManyToOne`, che in JPA sarebbero EAGER) e
  `spring.jpa.open-in-view=false`; ogni caso d'uso carica ciò che gli serve con query dedicate, senza mai mettere
  in JOIN FETCH più collezioni nella stessa query.
- **Transazioni**: service in `@Transactional(readOnly = true)`, scritture in `@Transactional`; la programmazione di
  una proiezione usa READ_COMMITTED più un lock pessimistico (`SELECT ... FOR UPDATE`) sulla sala, contro le
  programmazioni concorrenti sovrapposte.
- **Sicurezza**: form login con sessione, BCrypt, regole sugli URL e `@PreAuthorize` sui service, controllo della
  proprietà delle recensioni nel service; CSRF attivo anche per React (cookie `XSRF-TOKEN` → header `X-XSRF-TOKEN`).
- **Errori**: pagine 403/404 personalizzate per Thymeleaf, `ProblemDetail` (RFC 7807) per le API.

## Struttura del repository
```
├── pom.xml                         dipendenze, build del frontend (frontend-maven-plugin)
├── mvnw, mvnw.cmd, .mvn/           Maven wrapper
├── frontend/                       React + Vite (sorgenti in frontend/src)
└── src/
    ├── main/java/it/uniroma3/siw/festival/   codice Java (package descritti sopra)
    ├── main/resources/
    │   ├── application*.properties           configurazione (profilo dev)
    │   ├── templates/                         pagine Thymeleaf
    │   └── static/                            CSS; static/app è generata dalla build di React
    └── test/                                  test e profili test / experiment
```

## Licenza
MIT, vedi [LICENSE](LICENSE).
