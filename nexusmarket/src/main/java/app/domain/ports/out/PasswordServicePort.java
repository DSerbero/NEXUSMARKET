package app.domain.ports.out;

/**
 * Domain boundary for password hashing and verification. The domain depends on
 * this interface, never on a concrete hashing library (BCrypt, etc.), mirroring
 * the reference banking project. Implemented by a security adapter.
 */
public interface PasswordServicePort {

    String encrypt(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
