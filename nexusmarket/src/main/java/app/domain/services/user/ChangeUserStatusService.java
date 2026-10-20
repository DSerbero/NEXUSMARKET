package app.domain.services.user;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.User;
import app.domain.ports.out.UserRepositoryPort;
import app.domain.services.DomainValidations;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import lombok.RequiredArgsConstructor;

/**
 * Change User Status: only an ADMIN can change the account status (ACTIVE,
 * INACTIVE, BLOCKED) through a strict two-way transition.
 */
@Service
@ConditionalOnBean(UserRepositoryPort.class)
@RequiredArgsConstructor
public class ChangeUserStatusService {

    private final UserRepositoryPort userRepository;

    public User execute(User requestingUser, User targetUser, UserStatus status) {
        DomainValidations.active(requestingUser);
        requireAdmin(requestingUser);
        User current = userRepository.findByIdentifier(targetUser)
                .orElseThrow(() -> new DomainValidationException("User not found"));
        UserStatus nextStatus = DomainValidations.required(status, "status");
        if (!validStatusTransition(current.getStatus(), nextStatus)) {
            throw new DomainValidationException("Invalid user status transition");
        }
        current.setStatus(nextStatus);
        return userRepository.update(current);
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