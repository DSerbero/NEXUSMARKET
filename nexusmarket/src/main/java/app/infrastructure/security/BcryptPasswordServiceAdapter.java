package app.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import app.domain.ports.out.PasswordServicePort;
import lombok.RequiredArgsConstructor;

/**
 * Adapter that fulfils the domain {@link PasswordServicePort} with BCrypt. The
 * domain only knows the port; the hashing library is an infrastructure detail,
 * mirroring the reference banking project.
 */
@Component
@RequiredArgsConstructor
public class BcryptPasswordServiceAdapter implements PasswordServicePort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encrypt(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}