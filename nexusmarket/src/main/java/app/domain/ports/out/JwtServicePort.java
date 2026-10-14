package app.domain.ports.out;

import java.util.Optional;

import app.domain.models.User;
import app.domain.valueobjects.TokenClaims;

/**
 * Domain boundary for issuing and validating access tokens. The domain depends
 * on this interface and its own {@link TokenClaims} type, never on JJWT
 * directly (SDD: JWT details stay out of the domain). Implemented by a security
 * adapter.
 */
public interface JwtServicePort {

    String generateToken(User user);

    Optional<TokenClaims> parseToken(String token);
}
