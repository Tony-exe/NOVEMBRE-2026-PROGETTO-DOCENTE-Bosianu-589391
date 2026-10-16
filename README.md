# Festival cinematografici – Sistemi Informativi su Web

Progetto assegnato dal docente (SIW, Roma Tre, appello di novembre 2026).
Spring Boot 3.5 · JPA/Hibernate · PostgreSQL · Thymeleaf · React.

> README in costruzione: verrà completato nella fase 8.

## Prerequisiti
- JDK 21 o superiore (vedi `DECISIONI.md` per l'uso con JDK più recenti)
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
