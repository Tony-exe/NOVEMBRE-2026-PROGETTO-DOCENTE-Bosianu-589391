package it.uniroma3.siw.festival.exception;

/** 409: l'utente ha già recensito questo film. */
public class RecensioneDuplicataException extends RuntimeException {

    public RecensioneDuplicataException(String message) {
        super(message);
    }
}
