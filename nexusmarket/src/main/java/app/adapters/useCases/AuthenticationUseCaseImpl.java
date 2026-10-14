package app.adapters.useCases;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.User;
import app.domain.ports.in.AuthenticationPort;
import app.domain.ports.in.LoginResult;
import app.domain.ports.out.JwtServicePort;
import app.domain.services.user.AuthenticationService;
import lombok.RequiredArgsConstructor;

/**
 * Thin input adapter for authentication. It delegates credential validation to
 * the domain {@link AuthenticationService} and orchestrates token minting via
 * the {@link JwtServicePort}; the application layer, not the controller, is
 * where login and token issuance are composed.
 */
@Service
@ConditionalOnBean(AuthenticationService.class)
@RequiredArgsConstructor
public class AuthenticationUseCaseImpl implements AuthenticationPort {

    private final AuthenticationService authenticationService;
    private final JwtServicePort jwtServicePort;

    @Override
    public LoginResult login(String email, String password) {
        User user = authenticationService.login(email, password);
        return new LoginResult(user, jwtServicePort.generateToken(user));
    }
}
