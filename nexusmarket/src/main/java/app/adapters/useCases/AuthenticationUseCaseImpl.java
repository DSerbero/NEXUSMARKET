package app.adapters.useCases;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.User;
import app.domain.ports.in.AuthenticationPort;
import app.domain.services.user.AuthenticationService;
import lombok.RequiredArgsConstructor;

/**
 * Thin input adapter for authentication. It delegates entirely to the domain
 * {@link AuthenticationService} and adds no business rules.
 */
@Service
@ConditionalOnBean(AuthenticationService.class)
@RequiredArgsConstructor
public class AuthenticationUseCaseImpl implements AuthenticationPort {

    private final AuthenticationService authenticationService;

    @Override
    public User login(String email, String password) {
        return authenticationService.login(email, password);
    }
}
