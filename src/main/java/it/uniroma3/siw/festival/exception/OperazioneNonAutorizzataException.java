package it.uniroma3.siw.festival.exception;

/** 403: l'utente autenticato non può eseguire l'operazione (es. non è l'autore). */
public class OperazioneNonAutorizzataException extends RuntimeException {

    public OperazioneNonAutorizzataException(String message) {
        super(message);
    }
}
