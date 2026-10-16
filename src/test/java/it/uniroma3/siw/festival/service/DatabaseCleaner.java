package it.uniroma3.siw.festival.service;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * I test di integrazione dei service NON sono @Transactional (devono vedere i commit veri, il rollback
 * e la concorrenza tra thread), quindi i dati restano nel DB: li cancello dopo ogni test.
 */
final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    static void svuota(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE proiezione, recensione, interpretazione, festival_film, festival, film, "
                + "regista, attore, sala, utente RESTART IDENTITY CASCADE");
    }
}
