package it.uniroma3.siw.festival.exception;

/** Violazione di una regola di business (es. data fuori dal periodo del festival). */
public class RegolaBusinessException extends RuntimeException {

    public RegolaBusinessException(String message) {
        super(message);
    }
}
