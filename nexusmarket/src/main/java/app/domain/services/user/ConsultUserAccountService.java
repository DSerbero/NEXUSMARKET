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
 * Consult User Account: the account owner or an ADMIN/SUPERVISOR may read the
 * account profile.
 */
@Service
@ConditionalOnBean(UserRepositoryPort.class)
@RequiredArgsConstructor
public class ConsultUserAccountService {

    private final UserRepositoryPort userRepository;

    public User execute(User requestingUser, User targetUser) {
        DomainValidations.active(requestingUser);
        User current = userRepository.findByIdentifier(targetUser)
                .orElseThrow(() -> new DomainValidationException("User not found"));
        if (!DomainValidations.sameIdentity(requestingUser, current)
                && !UserRole.ADMIN.equals(requestingUser.getRole())
                && !UserRole.SUPERVISOR.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot consult this account");
        }
        return current;
    }
}