package app.domain.services.user;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.User;
import app.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Find User by Email: lookup the user by e-mail. Returns the authoritativo
 * (possibly empty) result without applying access rules; it is a read query
 * only and never throws.
 */
@Service
@ConditionalOnBean(UserRepositoryPort.class)
@RequiredArgsConstructor
public class FindUserByEmailService {

    private final UserRepositoryPort userRepository;

    public Optional<User> execute(User user) {
        return userRepository.findByEmail(user);
    }
}