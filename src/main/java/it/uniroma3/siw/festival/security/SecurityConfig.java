package it.uniroma3.siw.festival.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/*
 * Due livelli di autorizzazione:
 * 1) sugli URL, qui nella SecurityFilterChain (filtri che girano PRIMA dei controller);
 * 2) sui metodi dei service, con @PreAuthorize (abilitato da @EnableMethodSecurity), che protegge
 *    il caso d'uso anche se venisse chiamato da un altro controller o da un URL dimenticato.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
            // Le regole sono valutate nell'ordine: vince la prima che corrisponde
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                // scritture sulle recensioni: qualunque utente registrato (la proprietà si verifica nel service)
                .requestMatchers("/api/movies/*/reviews", "/api/reviews/**").hasAnyRole("USER", "ADMIN")
                // ogni altra scrittura via API è riservata all'amministratore
                .requestMatchers("/api/**").hasRole("ADMIN")
                // pagine pubbliche, login, registrazione, risorse statiche, React, Swagger
                .anyRequest().permitAll())
            .formLogin(form -> form
                .loginPage("/login")             // pagina Thymeleaf custom (GET); il POST lo gestisce il filtro
                .defaultSuccessUrl("/", false)   // false: dopo il login torna alla pagina richiesta, se c'era
                .failureUrl("/login?error=true") // credenziali errate: la pagina mostra il messaggio d'errore
                .permitAll())                    // la pagina di login deve essere pubblica (altrimenti loop)
            .logout(logout -> logout
                .logoutUrl("/logout")            // POST con token CSRF
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)     // distrugge la sessione sul server
                .deleteCookies("JSESSIONID")     // cancella il cookie di sessione nel browser
                .clearAuthentication(true)       // svuota il SecurityContext
                .permitAll())
            // CORS: usa il bean corsConfigurationSource definito sotto
            .cors(Customizer.withDefaults())
            /*
             * CSRF attivo anche per le API: React è servito dalla stessa origine e usa la stessa sessione
             * (cookie JSESSIONID), quindi una pagina malevola potrebbe sfruttarla. Il token sta nel cookie
             * XSRF-TOKEN, leggibile da JavaScript (withHttpOnlyFalse); React lo rimanda nell'header
             * X-XSRF-TOKEN. Un sito esterno non può leggere il cookie, quindi non può forgiare l'header.
             * CsrfTokenRequestAttributeHandler confronta il token "in chiaro": è il valore che React legge
             * dal cookie (il default di Spring 6 si aspetta invece un token mascherato a ogni richiesta).
             */
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint) // non autenticato → login o 401 JSON
                .accessDeniedHandler(accessDeniedHandler));         // non autorizzato → 403 o 403 JSON

        return http.build();
    }

    /*
     * CORS (lezione 12). Per la Same-Origin Policy il browser blocca le chiamate di una pagina servita da
     * http://localhost:5173 (server di sviluppo Vite) verso http://localhost:8080. Di norma non succede,
     * perché in sviluppo Vite fa da proxy e in produzione React è servito da Spring Boot (stessa origine);
     * questa configurazione autorizza comunque l'origine di Vite sulle API, cookie di sessione compresi.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true); // il browser può inviare il cookie JSESSIONID
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
