package app.domain.exceptions;

/**
 * The authenticated user lacks permission for the requested operation, or is
 * not active. Maps to HTTP 403 (or 401 where no identity is established).
 */
public class UnauthorizedOperationException extends DomainException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
