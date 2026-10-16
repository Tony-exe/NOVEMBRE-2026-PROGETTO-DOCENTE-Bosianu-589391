package it.uniroma3.siw.festival.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica con una vera richiesta HTTP (server avviato su una porta casuale) che il browser riceva
 * il cookie XSRF-TOKEN leggibile da JavaScript: è quello che React rimanda nell'header X-XSRF-TOKEN.
 * Non uso MockMvc perché il post-processor csrf() di spring-security-test, usato negli altri test,
 * modifica il repository CSRF del filtro nel contesto condiviso.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CsrfCookieTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void ilCookieXsrfTokenArrivaAlBrowserENonEHttpOnly() {
        ResponseEntity<String> risposta = rest.getForEntity("/login", String.class);

        List<String> cookies = risposta.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotNull();
        assertThat(cookies).anySatisfy(cookie -> {
            assertThat(cookie).startsWith("XSRF-TOKEN=");
            assertThat(cookie).doesNotContainIgnoringCase("HttpOnly");
        });
    }
}
