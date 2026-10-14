package app.domain.exceptions;

/**
 * Invalid input or a violated business rule that the caller can correct.
 * Maps to HTTP 400.
 */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }
}
