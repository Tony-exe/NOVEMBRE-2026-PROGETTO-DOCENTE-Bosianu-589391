package it.uniroma3.siw.festival;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FestivalApplicationTests {

    @Test
    void contextLoads() {
        // Verifica che il contesto Spring (entità, repository, configurazione) si avvii senza errori
    }
}
