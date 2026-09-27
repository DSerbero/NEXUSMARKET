package app.domain.services;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.User;
import app.domain.services.ports.UserRepositoryPort;
import app.domain.valueObjects.UserStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(UserRepositoryPort.class)
@RequiredArgsConstructor
public class UserService {

    private final UserRepositoryPort userRepository;

    public User consult(User requestingUser, User targetUser) {
        DomainValidations.active(requestingUser);
        return userRepository.findByIdentifier(targetUser).orElseThrow();
    }

    public User update(User requestingUser, User targetUser) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(targetUser, "targetUser");
        DomainValidations.requiredText(targetUser.getFullName(), "fullName");
        DomainValidations.requiredText(targetUser.getEmail(), "email");
        return userRepository.update(targetUser);
    }

    public User changeStatus(User requestingUser, User targetUser, UserStatus status) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(targetUser, "targetUser").setStatus(
                DomainValidations.required(status, "status"));
        return userRepository.update(targetUser);
    }

    public Optional<User> findByEmail(User user) {
        return userRepository.findByEmail(user);
    }
}
