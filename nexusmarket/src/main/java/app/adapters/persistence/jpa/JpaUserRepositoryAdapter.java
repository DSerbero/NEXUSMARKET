package app.adapters.persistence.jpa;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.UserJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.User;
import app.domain.ports.out.UserRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaUserRepositoryAdapter implements UserRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public User save(User user) {
        if (user == null) {
            throw new DomainValidationException("User must be provided");
        }
        if (user.getIdentifier() == null || user.getIdentifier().isBlank()) {
            user.setIdentifier(UUID.randomUUID().toString());
        }
        UserJpaEntity entity = entityManager.find(UserJpaEntity.class, user.getIdentifier());
        boolean isNew = entity == null;
        if (isNew) {
            entity = new UserJpaEntity();
            entity.setIdentifier(user.getIdentifier());
        }
        copyToEntity(user, entity);
        if (isNew) {
            entityManager.persist(entity);
        }
        return JpaDomainMapper.toUser(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(User user) {
        String email = normalizedEmail(user == null ? null : user.getEmail());
        if (email == null) {
            return Optional.empty();
        }
        return entityManager.createQuery(
                        "select user from UserJpaEntity user where user.normalizedEmail = :email", UserJpaEntity.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .map(JpaDomainMapper::toUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByIdentifier(User user) {
        if (user == null || user.getIdentifier() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(UserJpaEntity.class, user.getIdentifier()))
                .map(JpaDomainMapper::toUser);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(User user) {
        String email = normalizedEmail(user == null ? null : user.getEmail());
        if (email == null) {
            return false;
        }
        Long count = entityManager.createQuery(
                        "select count(user) from UserJpaEntity user where user.normalizedEmail = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    @Transactional
    public User update(User user) {
        if (user == null || user.getIdentifier() == null) {
            throw new DomainValidationException("User identifier is required");
        }
        UserJpaEntity entity = entityManager.find(UserJpaEntity.class, user.getIdentifier());
        if (entity == null) {
            throw new DomainValidationException("User not found");
        }
        copyToEntity(user, entity);
        return JpaDomainMapper.toUser(entity);
    }

    static String normalizedEmail(String email) {
        return email == null || email.isBlank() ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    static void copyToEntity(User user, UserJpaEntity entity) {
        entity.setFullName(user.getFullName());
        entity.setNormalizedEmail(normalizedEmail(user.getEmail()));
        entity.setRoleCode(user.getRole().getCode());
        entity.setStatusCode(user.getStatus().getCode());
    }
}