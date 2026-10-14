package app.domain.exceptions;

/**
 * Base class for every error raised by the domain or application layers.
 *
 * <p>Having a common ancestor lets the REST layer map domain failures to HTTP
 * status codes through a single {@code @RestControllerAdvice}, while specific
 * subclasses carry the semantics (not found, unauthorized, invalid input).</p>
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
