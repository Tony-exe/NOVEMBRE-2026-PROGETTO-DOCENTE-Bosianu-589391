package it.uniroma3.siw.festival.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * I test di integrazione dei service NON sono @Transactional (devono vedere i commit veri, il rollback
 * e la concorrenza tra thread), quindi i dati restano nel DB: li cancello dopo ogni test.
 */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void svuota(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE proiezione, recensione, interpretazione, festival_film, festival, film, "
                + "regista, attore, sala, utente RESTART IDENTITY CASCADE");
    }
}
