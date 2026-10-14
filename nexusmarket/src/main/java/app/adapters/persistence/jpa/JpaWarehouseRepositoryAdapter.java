package app.adapters.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.UserJpaEntity;
import app.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Warehouse;
import app.domain.ports.out.WarehouseRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persists {@link Warehouse}. Owner and responsible user are stored as logical
 * references to {@code users} and rehydrated on read so authorization rules
 * (ownership / logistics assignment) keep working off domain models.
 */
@Repository
public class JpaWarehouseRepositoryAdapter implements WarehouseRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Warehouse save(Warehouse warehouse) {
        if (warehouse == null) {
            throw new DomainValidationException("Warehouse must be provided");
        }
        DomainValidationsAdapter.requireText(warehouse.getLocation(), "location");
        DomainValidationsAdapter.require(warehouse.getOwnerType(), "ownerType");
        DomainValidationsAdapter.require(warehouse.getOwner(), "owner");

        WarehouseJpaEntity entity;
        boolean isNew = warehouse.getIdentifier() <= 0
                || entityManager.find(WarehouseJpaEntity.class, warehouse.getIdentifier()) == null;
        if (isNew) {
            entity = new WarehouseJpaEntity();
        } else {
            entity = entityManager.find(WarehouseJpaEntity.class, warehouse.getIdentifier());
        }
        entity.setLocation(warehouse.getLocation());
        entity.setOwnerType(warehouse.getOwnerType().getCode());
        entity.setOwnerIdentifier(warehouse.getOwner().getIdentifier());
        entity.setResponsibleUserIdentifier(warehouse.getResponsibleUser() == null
                ? null : warehouse.getResponsibleUser().getIdentifier());
        if (isNew) {
            entityManager.persist(entity);
        }
        entityManager.flush();
        warehouse.setIdentifier(entity.getIdentifier());
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warehouse> findByIdentifier(Warehouse warehouse) {
        if (warehouse == null || warehouse.getIdentifier() <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(WarehouseJpaEntity.class, warehouse.getIdentifier()))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findByOwner(Warehouse warehouse) {
        if (warehouse == null || warehouse.getOwner() == null
                || warehouse.getOwner().getIdentifier() == null) {
            return List.of();
        }
        return entityManager.createQuery(
                        "select w from WarehouseJpaEntity w where w.ownerIdentifier = :ownerIdentifier",
                        WarehouseJpaEntity.class)
                .setParameter("ownerIdentifier", warehouse.getOwner().getIdentifier())
                .getResultList()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Warehouse update(Warehouse warehouse) {
        if (warehouse == null || warehouse.getIdentifier() <= 0) {
            throw new DomainValidationException("Warehouse identifier is required");
        }
        WarehouseJpaEntity entity = entityManager.find(WarehouseJpaEntity.class, warehouse.getIdentifier());
        if (entity == null) {
            throw new DomainValidationException("Warehouse not found");
        }
        entity.setLocation(warehouse.getLocation());
        entity.setOwnerType(warehouse.getOwnerType().getCode());
        entity.setOwnerIdentifier(warehouse.getOwner().getIdentifier());
        entity.setResponsibleUserIdentifier(warehouse.getResponsibleUser() == null
                ? null : warehouse.getResponsibleUser().getIdentifier());
        entityManager.flush();
        return toDomain(entity);
    }

    private Warehouse toDomain(WarehouseJpaEntity entity) {
        UserJpaEntity owner = entity.getOwnerIdentifier() == null
                ? null : entityManager.find(UserJpaEntity.class, entity.getOwnerIdentifier());
        UserJpaEntity responsible = entity.getResponsibleUserIdentifier() == null
                ? null : entityManager.find(UserJpaEntity.class, entity.getResponsibleUserIdentifier());
        return JpaDomainMapper.toWarehouse(entity, owner, responsible);
    }
}
