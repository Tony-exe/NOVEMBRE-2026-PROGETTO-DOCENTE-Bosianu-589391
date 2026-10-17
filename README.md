# Festival cinematografici – Sistemi Informativi su Web

Progetto assegnato dal docente (SIW, Roma Tre, appello di novembre 2026).
Spring Boot 3.5 · JPA/Hibernate · PostgreSQL · Thymeleaf · React.

> README in costruzione: verrà completato nella fase 8.

## Prerequisiti
- JDK 21 o superiore. Con JDK più recenti di Java 21 il `pom.xml` abilita
  `-Dnet.bytebuddy.experimental=true`, necessario a Hibernate e Mockito per creare proxy e mock.
- PostgreSQL in locale sulla porta 5432, utente `postgres`, password `postgres`, con due database:
  ```sql
  CREATE DATABASE festival;
  CREATE DATABASE festival_test;
  ```
- Maven non serve: si usa il wrapper `mvnw`.

## Avvio
```bash
./mvnw spring-boot:run
```
Al primo avvio (profilo `dev`) `DataInitializer` inserisce i dati di esempio.

## Test
```bash
./mvnw verify
```

## Credenziali di esempio
| Username | Password | Ruolo |
|---|---|---|
| admin | admin | ADMIN |
| user1 | user1 | USER |
| user2 | user2 | USER |

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

## Licenza
MIT, vedi [LICENSE](LICENSE).
