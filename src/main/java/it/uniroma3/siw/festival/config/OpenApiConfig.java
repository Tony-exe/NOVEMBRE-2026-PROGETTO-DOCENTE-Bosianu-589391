package it.uniroma3.siw.festival.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Metadati della documentazione OpenAPI. springdoc genera la specifica leggendo i @RestController
 * (JSON su /v3/api-docs, interfaccia su /swagger-ui.html).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI festivalOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Festival cinematografici – API REST")
                .version("1.0")
                .description("API usate dal frontend React. Le letture sono pubbliche; per scrivere recensioni "
                        + "serve il login (sessione) e il token CSRF nell'header X-XSRF-TOKEN."));
    }
}
