package app.adapters.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.OrderJpaEntity;
import app.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import app.adapters.persistence.jpa.entities.UserJpaEntity;
import app.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Order;
import app.domain.models.Shipment;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.ports.out.ShipmentRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persists {@link Shipment}. Order, origin warehouse and operator are referenced
 * by identifier and rehydrated so logistics rules and authorization keep working.
 */
@Repository
public class JpaShipmentRepositoryAdapter implements ShipmentRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Shipment save(Shipment shipment) {
        DomainValidationsAdapter.require(shipment, "shipment");
        DomainValidationsAdapter.require(shipment.getOrder(), "order");
        DomainValidationsAdapter.require(shipment.getOriginWarehouse(), "originWarehouse");
        DomainValidationsAdapter.require(shipment.getOperator(), "operator");
        if (shipment.getIdentifier() == null || shipment.getIdentifier().isBlank()) {
            shipment.setIdentifier(UUID.randomUUID().toString());
        }
        OrderJpaEntity order = entityManager.find(OrderJpaEntity.class, shipment.getOrder().getIdentifier());
        if (order == null) {
            throw new DomainValidationException("Order not found");
        }
        ShipmentJpaEntity entity = new ShipmentJpaEntity();
        entity.setIdentifier(shipment.getIdentifier());
        entity.setOrderIdentifier(order.getIdentifier());
        entity.setOriginWarehouseIdentifier(shipment.getOriginWarehouse().getIdentifier());
        entity.setOperatorIdentifier(shipment.getOperator().getIdentifier());
        entity.setStatusCode(shipment.getShipmentStatus().getCode());
        entity.setDispatchDate(shipment.getDispatchDate());
        entityManager.persist(entity);
        entityManager.flush();
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Shipment> findByOrder(Shipment shipment) {
        if (shipment == null || shipment.getOrder() == null
                || shipment.getOrder().getIdentifier() == null) {
            return Optional.empty();
        }
        return entityManager.createQuery(
                        "select s from ShipmentJpaEntity s where s.orderIdentifier = :orderIdentifier",
                        ShipmentJpaEntity.class)
                .setParameter("orderIdentifier", shipment.getOrder().getIdentifier())
                .getResultStream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Shipment> findByIdentifier(Shipment shipment) {
        if (shipment == null || shipment.getIdentifier() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(ShipmentJpaEntity.class, shipment.getIdentifier()))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shipment> findByOperator(Shipment shipment) {
        if (shipment == null || shipment.getOperator() == null
                || shipment.getOperator().getIdentifier() == null) {
            return List.of();
        }
        return entityManager.createQuery(
                        "select s from ShipmentJpaEntity s where s.operatorIdentifier = :operatorIdentifier",
                        ShipmentJpaEntity.class)
                .setParameter("operatorIdentifier", shipment.getOperator().getIdentifier())
                .getResultList()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Shipment update(Shipment shipment) {
        if (shipment == null || shipment.getIdentifier() == null) {
            throw new DomainValidationException("Shipment identifier is required");
        }
        ShipmentJpaEntity entity = entityManager.find(ShipmentJpaEntity.class, shipment.getIdentifier());
        if (entity == null) {
            throw new DomainValidationException("Shipment not found");
        }
        entity.setStatusCode(shipment.getShipmentStatus().getCode());
        entity.setDispatchDate(shipment.getDispatchDate());
        entityManager.flush();
        return toDomain(entity);
    }

    private Shipment toDomain(ShipmentJpaEntity entity) {
        Shipment shipment = new Shipment();
        shipment.setIdentifier(entity.getIdentifier());
        shipment.setOrder(toOrder(entity.getOrderIdentifier()));
        shipment.setOriginWarehouse(toWarehouse(entity.getOriginWarehouseIdentifier()));
        shipment.setOperator(toOperator(entity.getOperatorIdentifier()));
        shipment.setShipmentStatus(JpaDomainMapper.shipmentStatus(entity.getStatusCode()));
        shipment.setDispatchDate(entity.getDispatchDate());
        return shipment;
    }

    private Order toOrder(String orderIdentifier) {
        OrderJpaEntity order = entityManager.find(OrderJpaEntity.class, orderIdentifier);
        if (order == null) {
            throw new DomainValidationException("Order not found");
        }
        Order domain = new Order();
        domain.setIdentifier(order.getIdentifier());
        domain.setOrderStatus(JpaDomainMapper.orderStatus(order.getStatusCode()));
        return domain;
    }

    private User toOperator(String operatorIdentifier) {
        UserJpaEntity operator = entityManager.find(UserJpaEntity.class, operatorIdentifier);
        if (operator == null) {
            throw new DomainValidationException("Operator not found");
        }
        return JpaDomainMapper.toUser(operator);
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
