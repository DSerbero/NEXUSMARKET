package application.domain.ports.out;

import java.util.Optional;

import application.domain.models.User;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByEmail(User user);
    Optional<User> findByIdentifier(User user);
    boolean existsByEmail(User user);
    User update(User user);
}
