package app.adapters.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.InventoryJpaEntity;
import app.adapters.persistence.jpa.entities.UserJpaEntity;
import app.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Inventory;
import app.domain.models.PhysicalProduct;
import app.domain.models.Warehouse;
import app.domain.ports.out.InventoryRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persists {@link Inventory} (physical stock per product/warehouse). The product
 * lives in MongoDB, so only a logical {@code product_identifier} is stored; the
 * warehouse is rehydrated fully so inventory authorization keeps working.
 */
@Repository
public class JpaInventoryRepositoryAdapter implements InventoryRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Inventory save(Inventory inventory) {
        DomainValidationsAdapter.require(inventory, "inventory");
        DomainValidationsAdapter.require(inventory.getProduct(), "product");
        DomainValidationsAdapter.require(inventory.getWarehouse(), "warehouse");
        if (inventory.getIdentifier() == null || inventory.getIdentifier().isBlank()) {
            inventory.setIdentifier(UUID.randomUUID().toString());
        }
        InventoryJpaEntity entity = new InventoryJpaEntity();
        entity.setIdentifier(inventory.getIdentifier());
        entity.setProductIdentifier(inventory.getProduct().getIdentifier());
        entity.setWarehouseIdentifier(inventory.getWarehouse().getIdentifier());
        entity.setAvailableQuantity(inventory.getAvailableQuantity());
        entity.setMovementType(inventory.getMovementType().getCode());
        entity.setMovementDate(inventory.getMovementDate());
        entityManager.persist(entity);
        entityManager.flush();
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Inventory> findByProductAndWarehouse(Inventory inventory) {
        if (inventory == null || inventory.getProduct() == null || inventory.getWarehouse() == null
                || inventory.getProduct().getIdentifier() == null
                || inventory.getWarehouse().getIdentifier() <= 0) {
            return Optional.empty();
        }
        return entityManager.createQuery(
                        "select i from InventoryJpaEntity i where i.productIdentifier = :productIdentifier "
                                + "and i.warehouseIdentifier = :warehouseIdentifier",
                        InventoryJpaEntity.class)
                .setParameter("productIdentifier", inventory.getProduct().getIdentifier())
                .setParameter("warehouseIdentifier", inventory.getWarehouse().getIdentifier())
                .getResultStream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findByWarehouse(Inventory inventory) {
        if (inventory == null || inventory.getWarehouse() == null
                || inventory.getWarehouse().getIdentifier() <= 0) {
            return List.of();
        }
        return entityManager.createQuery(
                        "select i from InventoryJpaEntity i where i.warehouseIdentifier = :warehouseIdentifier",
                        InventoryJpaEntity.class)
                .setParameter("warehouseIdentifier", inventory.getWarehouse().getIdentifier())
                .getResultList()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Inventory update(Inventory inventory) {
        if (inventory == null || inventory.getIdentifier() == null) {
            throw new DomainValidationException("Inventory identifier is required");
        }
        InventoryJpaEntity entity = entityManager.find(InventoryJpaEntity.class, inventory.getIdentifier());
        if (entity == null) {
            throw new DomainValidationException("Inventory not found");
        }
        entity.setAvailableQuantity(inventory.getAvailableQuantity());
        entity.setMovementType(inventory.getMovementType().getCode());
        entity.setMovementDate(inventory.getMovementDate());
        entityManager.flush();
        return toDomain(entity);
    }

    private Inventory toDomain(InventoryJpaEntity entity) {
        Inventory inventory = new Inventory();
        inventory.setIdentifier(entity.getIdentifier());
        PhysicalProduct product = new PhysicalProduct();
        product.setIdentifier(entity.getProductIdentifier());
        inventory.setProduct(product);
        inventory.setWarehouse(toWarehouse(entity.getWarehouseIdentifier()));
        inventory.setAvailableQuantity(entity.getAvailableQuantity());
        inventory.setMovementType(JpaDomainMapper.movementType(entity.getMovementType()));
        inventory.setMovementDate(entity.getMovementDate());
        return inventory;
    }

    private Warehouse toWarehouse(Long warehouseIdentifier) {
        WarehouseJpaEntity warehouseEntity = entityManager.find(WarehouseJpaEntity.class, warehouseIdentifier);
        if (warehouseEntity == null) {
            throw new DomainValidationException("Warehouse not found");
        }
        UserJpaEntity owner = warehouseEntity.getOwnerIdentifier() == null
                ? null : entityManager.find(UserJpaEntity.class, warehouseEntity.getOwnerIdentifier());
        UserJpaEntity responsible = warehouseEntity.getResponsibleUserIdentifier() == null
                ? null : entityManager.find(UserJpaEntity.class, warehouseEntity.getResponsibleUserIdentifier());
        return JpaDomainMapper.toWarehouse(warehouseEntity, owner, responsible);
    }
}
