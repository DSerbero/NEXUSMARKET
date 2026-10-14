package app.domain.services.user;

import app.domain.services.DomainValidations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.InvalidCredentialsException;
import app.domain.models.User;
import app.domain.ports.out.SecurityPort;
import app.domain.valueobjects.UserStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(SecurityPort.class)
@RequiredArgsConstructor
public class AuthenticationService {

    private final SecurityPort securityPort;

    public User login(String email, String password) {
        DomainValidations.requiredText(email, "email");
        DomainValidations.requiredText(password, "password");
        User user = securityPort.authenticate(email, password);
        if (user == null || !UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    public void logout(User user) {
        DomainValidations.active(user);
        securityPort.logout(user);
    }
}
