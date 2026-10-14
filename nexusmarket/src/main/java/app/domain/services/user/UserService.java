package app.domain.services.user;

import app.domain.services.DomainValidations;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.User;
import app.domain.exceptions.DomainValidationException;
import app.domain.ports.out.UserRepositoryPort;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(UserRepositoryPort.class)
@RequiredArgsConstructor
public class UserService {

    private final UserRepositoryPort userRepository;

    public User consult(User requestingUser, User targetUser) {
        DomainValidations.active(requestingUser);
        User current = authoritative(targetUser);
        if (!DomainValidations.sameIdentity(requestingUser, current)
                && !UserRole.ADMIN.equals(requestingUser.getRole())
                && !UserRole.SUPERVISOR.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot consult this account");
        }
        return current;
    }

    public User update(User requestingUser, User targetUser) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(targetUser, "targetUser");
        DomainValidations.requiredText(targetUser.getFullName(), "fullName");
        DomainValidations.requiredText(targetUser.getEmail(), "email");
        User current = authoritative(targetUser);
        if (!DomainValidations.sameIdentity(requestingUser, current)
                && !UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot update this account");
        }
        User emailQuery = new User();
        emailQuery.setEmail(targetUser.getEmail());
        userRepository.findByEmail(emailQuery).ifPresent(existing -> {
            if (!DomainValidations.sameIdentity(existing, current)) {
                throw new DomainValidationException("Email already exists");
            }
        });
        current.setFullName(targetUser.getFullName());
        current.setEmail(targetUser.getEmail());
        return userRepository.update(current);
    }

    public User changeStatus(User requestingUser, User targetUser, UserStatus status) {
        DomainValidations.active(requestingUser);
        requireAdmin(requestingUser);
        User current = authoritative(targetUser);
        UserStatus nextStatus = DomainValidations.required(status, "status");
        if (!validStatusTransition(current.getStatus(), nextStatus)) {
            throw new DomainValidationException("Invalid user status transition");
        }
        current.setStatus(nextStatus);
        return userRepository.update(current);
    }

    public Optional<User> findByEmail(User user) {
        return userRepository.findByEmail(user);
    }

    private User authoritative(User targetUser) {
        DomainValidations.required(targetUser, "targetUser");
        return userRepository.findByIdentifier(targetUser)
                .orElseThrow(() -> new DomainValidationException("User not found"));
    }

    private void requireAdmin(User user) {
        if (!UserRole.ADMIN.equals(user.getRole())) {
            throw new DomainValidationException("Only an admin can change user status");
        }
    }

    private boolean validStatusTransition(UserStatus current, UserStatus next) {
        return (UserStatus.ACTIVE.equals(current)
                        && (UserStatus.INACTIVE.equals(next) || UserStatus.BLOCKED.equals(next)))
                || ((UserStatus.INACTIVE.equals(current) || UserStatus.BLOCKED.equals(current))
                        && UserStatus.ACTIVE.equals(next));
    }
}
