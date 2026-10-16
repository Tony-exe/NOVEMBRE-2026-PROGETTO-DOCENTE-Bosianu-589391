package it.uniroma3.siw.festival.exception;

/**
 * 404: l'entità richiesta non esiste.
 *
 * Tutte le eccezioni di dominio estendono RuntimeException (unchecked): per default @Transactional
 * fa rollback solo sulle eccezioni unchecked, mentre con una checked la transazione verrebbe
 * confermata (commit) nonostante l'errore.
 * Nota: è diversa da jakarta.persistence.EntityNotFoundException, che è un'eccezione di JPA.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entita, Long id) {
        super(entita + " con id " + id + " non trovato");
    }
}
