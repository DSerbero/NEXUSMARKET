package app.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import app.domain.ports.out.PasswordServicePort;

class BcryptPasswordServiceAdapterTest {

    private final PasswordServicePort passwordService =
            new BcryptPasswordServiceAdapter(new BCryptPasswordEncoder());

    @Test
    void encryptProducesSaltedDifferentHashesForSamePassword() {
        String first = passwordService.encrypt("SuperSecret123!");
        String second = passwordService.encrypt("SuperSecret123!");

        assertNotEquals(first, second);
    }

    @Test
    void matchingPasswordIsAcceptedAndWrongOneIsRejected() {
        String hash = passwordService.encrypt("SuperSecret123!");

        assertTrue(passwordService.matches("SuperSecret123!", hash));
        assertFalse(passwordService.matches("nope", hash));
    }
}