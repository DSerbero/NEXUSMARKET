package app.adapters.persistence.jpa;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.BuyerJpaEntity;
import app.adapters.persistence.jpa.entities.UserJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.ports.out.BuyerRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaBuyerRepositoryAdapter implements BuyerRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Buyer save(Buyer buyer) {
        if (buyer == null) {
            throw new DomainValidationException("Buyer must be provided");
        }
        if (buyer.getIdentifier() == null || buyer.getIdentifier().isBlank()) {
            buyer.setIdentifier(UUID.randomUUID().toString());
        }
        UserJpaEntity user = entityManager.find(UserJpaEntity.class, buyer.getIdentifier());
        boolean newUser = user == null;
        if (newUser) {
            user = new UserJpaEntity();
            user.setIdentifier(buyer.getIdentifier());
        }
        JpaUserRepositoryAdapter.copyToEntity(buyer, user);
        if (newUser) {
            entityManager.persist(user);
        }

        BuyerJpaEntity profile = entityManager.find(BuyerJpaEntity.class, buyer.getIdentifier());
        boolean newProfile = profile == null;
        if (newProfile) {
            profile = new BuyerJpaEntity();
            profile.setUserIdentifier(buyer.getIdentifier());
        }
        profile.setUser(user);
        profile.setPrimaryAddress(buyer.getPrimaryAddress());
        profile.setBuyerStatusCode(buyer.getBuyerStatus().getCode());
        profile.setAdditionalAddresses(buyer.getAdditionalAddresses() == null
                ? new ArrayList<>() : new ArrayList<>(buyer.getAdditionalAddresses()));
        if (newProfile) {
            entityManager.persist(profile);
        }
        entityManager.flush();
        return JpaDomainMapper.toBuyer(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Buyer> findByIdentifier(Buyer buyer) {
        if (buyer == null || buyer.getIdentifier() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(BuyerJpaEntity.class, buyer.getIdentifier()))
                .map(JpaDomainMapper::toBuyer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Buyer> findByEmail(Buyer buyer) {
        String email = JpaUserRepositoryAdapter.normalizedEmail(buyer == null ? null : buyer.getEmail());
        if (email == null) {
            return Optional.empty();
        }
        return entityManager.createQuery(
                        "select buyer from BuyerJpaEntity buyer join fetch buyer.user user "
                                + "where user.normalizedEmail = :email",
                        BuyerJpaEntity.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .map(JpaDomainMapper::toBuyer);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Buyer buyer) {
        String email = JpaUserRepositoryAdapter.normalizedEmail(buyer == null ? null : buyer.getEmail());
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
    public Buyer update(Buyer buyer) {
        if (buyer == null || buyer.getIdentifier() == null) {
            throw new DomainValidationException("Buyer identifier is required");
        }
        return save(buyer);
    }
}