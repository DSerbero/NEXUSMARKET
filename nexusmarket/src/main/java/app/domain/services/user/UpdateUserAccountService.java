package app.domain.services.user;

import java.util.Optional;

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
 * Update User Account: the account owner or an ADMIN/SUPERVISOR may refresh the
 * profile. Email stays unique across the marketplace.
 */
@Service
@ConditionalOnBean(UserRepositoryPort.class)
@RequiredArgsConstructor
public class UpdateUserAccountService {

    private final UserRepositoryPort userRepository;

    public User execute(User requestingUser, User targetUser) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(targetUser, "targetUser");
        DomainValidations.requiredText(targetUser.getFullName(), "fullName");
        DomainValidations.requiredText(targetUser.getEmail(), "email");
        User current = userRepository.findByIdentifier(targetUser)
                .orElseThrow(() -> new DomainValidationException("User not found"));
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
}