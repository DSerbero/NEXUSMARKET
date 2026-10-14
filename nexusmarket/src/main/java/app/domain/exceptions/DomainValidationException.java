package app.domain.exceptions;

/**
 * Legacy validation exception kept for backward compatibility. It now extends
 * {@link ValidationException} (which maps to HTTP 400) so existing throw sites
 * keep compiling and existing tests keep passing, while the REST layer can treat
 * it uniformly through the new exception hierarchy.
 */
public class DomainValidationException extends ValidationException {

    public DomainValidationException(String message) {
        super(message);
    }
}

