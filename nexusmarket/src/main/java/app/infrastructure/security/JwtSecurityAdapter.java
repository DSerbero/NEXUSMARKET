package app.infrastructure.security;

import org.springframework.stereotype.Repository;

import app.adapters.persistence.jpa.repositories.UserCredentialJpaRepository;
import app.domain.models.User;
import app.domain.ports.out.PasswordServicePort;
import app.domain.ports.out.SecurityPort;
import app.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Implements the domain {@link SecurityPort}. Verifies credentials against the
 * persisted password hash and returns the authoritative domain {@link User}.
 *
 * <p>Password comparison goes through {@link PasswordServicePort} so even the
 * hashing library stays outside the domain, exactly as the SDD requires.</p>
 */
@Repository
@RequiredArgsConstructor
public class JwtSecurityAdapter implements SecurityPort {

    private final UserRepositoryPort userRepository;
    private final UserCredentialJpaRepository credentialRepository;
    private final PasswordServicePort passwordService;

    @Override
    public User authenticate(String email, String password) {
        if (email == null || password == null) {
            return null;
        }
        User probe = new User();
        probe.setEmail(email);
        return userRepository.findByEmail(probe)
                .filter(user -> credentialRepository.findById(user.getIdentifier())
                        .map(credential -> passwordService.matches(password, credential.getPasswordHash()))
                        .orElse(false))
                .orElse(null);
    }

    @Override
    public void logout(User user) {
        // Stateless JWT: the client discards the token. Server-side revocation
        // (deny-list) is intentionally out of scope for this security slice.
    }
}
