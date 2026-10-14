package application.adapters.useCases.user;

import application.domain.services.DomainValidations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import application.domain.models.User;
import application.domain.exceptions.DomainValidationException;
import application.domain.ports.out.SecurityPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;
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
            throw new application.domain.exceptions.DomainValidationException("Invalid credentials or inactive user");
        }
        return user;
    }

    public void logout(User user) {
        DomainValidations.active(user);
        securityPort.logout(user);
    }

    public User changeStatus(User requestingUser, User targetUser, UserStatus status) {
        DomainValidations.active(requestingUser);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("Only an admin can change user status");
        }
        DomainValidations.required(targetUser, "targetUser");
        User current = userRepository.findByIdentifier(targetUser)
                .orElseThrow(() -> new DomainValidationException("User not found"));
        UserStatus nextStatus = DomainValidations.required(status, "status");
        boolean validTransition = (UserStatus.ACTIVE.equals(current.getStatus())
                        && (UserStatus.INACTIVE.equals(nextStatus) || UserStatus.BLOCKED.equals(nextStatus)))
                || ((UserStatus.INACTIVE.equals(current.getStatus()) || UserStatus.BLOCKED.equals(current.getStatus()))
                        && UserStatus.ACTIVE.equals(nextStatus));
        if (!validTransition) {
            throw new DomainValidationException("Invalid user status transition");
        }
        current.setStatus(nextStatus);
        return userRepository.update(current);
    }
}
