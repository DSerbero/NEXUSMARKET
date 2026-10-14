package app.domain.exceptions;

/**
 * Credentials are wrong or the account is not allowed to sign in. Maps to
 * HTTP 401. The message is intentionally generic to avoid user enumeration.
 */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid credentials or inactive user");
    }
}
