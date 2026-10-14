package app.domain.exceptions;

/**
 * A referenced domain entity does not exist. Maps to HTTP 404.
 */
public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entity) {
        super(entity + " not found");
    }
}
