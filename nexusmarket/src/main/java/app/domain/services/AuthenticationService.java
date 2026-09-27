package app.domain.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.User;
import app.domain.services.ports.SecurityPort;
import app.domain.services.ports.UserRepositoryPort;
import app.domain.valueObjects.UserStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(SecurityPort.class)
@RequiredArgsConstructor
public class AuthenticationService {

    private final SecurityPort securityPort;
    private final UserRepositoryPort userRepository;

    public User login(String email, String password) {
        DomainValidations.requiredText(email, "email");
        DomainValidations.requiredText(password, "password");
        User user = securityPort.authenticate(email, password);
        if (user == null || !UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new app.domain.services.exceptions.DomainValidationException("Invalid credentials or inactive user");
        }
        return user;
    }

    public void logout(User user) {
        DomainValidations.active(user);
        securityPort.logout(user);
    }

    public User changeStatus(User requestingUser, User targetUser, UserStatus status) {
        DomainValidations.active(requestingUser);
        targetUser.setStatus(DomainValidations.required(status, "status"));
        return userRepository.update(targetUser);
    }
}
