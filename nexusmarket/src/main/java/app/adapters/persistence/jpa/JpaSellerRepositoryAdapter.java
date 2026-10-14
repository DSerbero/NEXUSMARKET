package app.adapters.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.SellerJpaEntity;
import app.adapters.persistence.jpa.entities.UserJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Seller;
import app.domain.ports.out.SellerRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persists {@link Seller} as a 1:1 profile sharing the {@code users} identity,
 * exactly like {@link JpaBuyerRepositoryAdapter}. The email unique constraint
 * lives on {@code users}, preventing duplicates across roles.
 */
@Repository
public class JpaSellerRepositoryAdapter implements SellerRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Seller save(Seller seller) {
        if (seller == null) {
            throw new DomainValidationException("Seller must be provided");
        }
        if (seller.getIdentifier() == null || seller.getIdentifier().isBlank()) {
            seller.setIdentifier(UUID.randomUUID().toString());
        }
        UserJpaEntity user = entityManager.find(UserJpaEntity.class, seller.getIdentifier());
        boolean newUser = user == null;
        if (newUser) {
            user = new UserJpaEntity();
            user.setIdentifier(seller.getIdentifier());
        }
        JpaUserRepositoryAdapter.copyToEntity(seller, user);
        if (newUser) {
            entityManager.persist(user);
        }

        SellerJpaEntity profile = entityManager.find(SellerJpaEntity.class, seller.getIdentifier());
        if (profile == null) {
            profile = new SellerJpaEntity();
            profile.setUserIdentifier(seller.getIdentifier());
            profile.setUser(user);
            entityManager.persist(profile);
        }
        entityManager.flush();
        return JpaDomainMapper.toSeller(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Seller> findByIdentifier(Seller seller) {
        if (seller == null || seller.getIdentifier() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(SellerJpaEntity.class, seller.getIdentifier()))
                .map(JpaDomainMapper::toSeller);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Seller> findByEmail(Seller seller) {
        String email = JpaUserRepositoryAdapter.normalizedEmail(seller == null ? null : seller.getEmail());
        if (email == null) {
            return Optional.empty();
        }
        return entityManager.createQuery(
                        "select seller from SellerJpaEntity seller join fetch seller.user user "
                                + "where user.normalizedEmail = :email",
                        SellerJpaEntity.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .map(JpaDomainMapper::toSeller);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Seller seller) {
        String email = JpaUserRepositoryAdapter.normalizedEmail(seller == null ? null : seller.getEmail());
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
    public Seller update(Seller seller) {
        if (seller == null || seller.getIdentifier() == null) {
            throw new DomainValidationException("Seller identifier is required");
        }
        return save(seller);
    }
}
