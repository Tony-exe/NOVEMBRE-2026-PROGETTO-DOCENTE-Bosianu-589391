package it.uniroma3.siw.festival.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw.festival.dto.form.RegistrazioneForm;
import it.uniroma3.siw.festival.exception.EntityNotFoundException;
import it.uniroma3.siw.festival.exception.UsernameGiaUsatoException;
import it.uniroma3.siw.festival.model.Ruolo;
import it.uniroma3.siw.festival.model.Utente;
import it.uniroma3.siw.festival.repository.UtenteRepository;

@Service
@Transactional(readOnly = true)
public class UtenteService {

    private static final Logger log = LoggerFactory.getLogger(UtenteService.class);

    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;

    public UtenteService(UtenteRepository utenteRepository, PasswordEncoder passwordEncoder) {
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Utente trova(String username) {
        return utenteRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Utente " + username + " non trovato"));
    }

    /** Registra un nuovo utente con ruolo USER (gli admin non si registrano dal sito). */
    @Transactional
    public Utente registra(RegistrazioneForm form) {
        String username = form.getUsername().trim();
        // Il vincolo unique su username nel DB resta comunque l'ultima garanzia
        if (utenteRepository.existsByUsername(username)) {
            log.warn("Registrazione rifiutata: username {} già in uso", username);
            throw new UsernameGiaUsatoException("Lo username \"" + username + "\" è già in uso");
        }
        // Nel DB va solo l'hash BCrypt, mai la password in chiaro
        Utente utente = new Utente(username, passwordEncoder.encode(form.getPassword()), Ruolo.USER);
        Utente salvato = utenteRepository.save(utente);
        log.info("Utente registrato: {}", username); // mai la password nel log
        return salvato;
    }
}
