package app.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.domain.models.User;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import app.domain.valueobjects.TokenClaims;

class JwtServiceTest {

    // >= 32 bytes for HS256.
    private static final String SECRET = "unit-test-secret-key-0123456789-abcdefghijklmnop";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    private User user() {
        User user = new User();
        user.setIdentifier("user-1");
        user.setEmail("user@example.test");
        user.setRole(UserRole.BUYER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    void generatedTokenCarriesSubjectAndRoleAndCanBeParsed() {
        String token = jwtService.generateToken(user());

        Optional<TokenClaims> claims = jwtService.parseToken(token);

        assertTrue(claims.isPresent());
        assertEquals("user-1", claims.get().subject());
        assertEquals("BUYER", claims.get().role());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiredService = new JwtService(SECRET, -1_000L);

        String token = expiredService.generateToken(user());

        assertFalse(jwtService.parseToken(token).isPresent());
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user());
        String tampered = token.substring(0, token.length() - 2) + "aa";

        assertFalse(jwtService.parseToken(tampered).isPresent());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService otherSigner = new JwtService("a-completely-different-secret-value-0123456789", 3_600_000L);
        String foreignToken = otherSigner.generateToken(user());

        assertFalse(jwtService.parseToken(foreignToken).isPresent());
    }

    @Test
    void garbageTokenIsRejected() {
        assertFalse(jwtService.parseToken("not-a-jwt").isPresent());
        assertFalse(jwtService.parseToken("").isPresent());
        assertFalse(jwtService.parseToken(null).isPresent());
    }
}
