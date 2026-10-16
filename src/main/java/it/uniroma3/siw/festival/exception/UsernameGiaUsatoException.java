package it.uniroma3.siw.festival.exception;

/** Registrazione con uno username già esistente. */
public class UsernameGiaUsatoException extends RuntimeException {

    public UsernameGiaUsatoException(String message) {
        super(message);
    }
}
