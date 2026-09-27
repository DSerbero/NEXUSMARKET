package app.domain.services.ports;

import java.util.Optional;

import app.domain.models.User;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByEmail(User user);
    Optional<User> findByIdentifier(User user);
    boolean existsByEmail(User user);
    User update(User user);
}
