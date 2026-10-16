package it.uniroma3.siw.festival.exception;

/** 409: la sala è già occupata nell'intervallo richiesto. */
public class ConflittoProgrammazioneException extends RuntimeException {

    public ConflittoProgrammazioneException(String message) {
        super(message);
    }
}
