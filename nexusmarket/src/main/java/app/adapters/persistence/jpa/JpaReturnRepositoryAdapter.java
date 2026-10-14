package app.adapters.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.OrderJpaEntity;
import app.adapters.persistence.jpa.entities.ReturnJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Order;
import app.domain.models.Return;
import app.domain.valueobjects.ReturnStatus;
import app.domain.ports.out.ReturnRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persists {@link Return}. "Active" means the return is still open (REQUESTED or
 * APPROVED); REJECTED and COMPLETED are terminal and do not block a new request.
 */
@Repository
public class JpaReturnRepositoryAdapter implements ReturnRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Return save(Return returnRequest) {
        DomainValidationsAdapter.require(returnRequest, "return");
        DomainValidationsAdapter.require(returnRequest.getOrder(), "order");
        if (returnRequest.getIdentifier() == null || returnRequest.getIdentifier().isBlank()) {
            returnRequest.setIdentifier(UUID.randomUUID().toString());
        }
        OrderJpaEntity order = entityManager.find(OrderJpaEntity.class, returnRequest.getOrder().getIdentifier());
        if (order == null) {
            throw new DomainValidationException("Order not found");
        }
        ReturnJpaEntity entity = new ReturnJpaEntity();
        entity.setIdentifier(returnRequest.getIdentifier());
        entity.setOrderIdentifier(order.getIdentifier());
        entity.setReason(returnRequest.getReason());
        entity.setStatusCode(returnRequest.getReturnStatus().getCode());
        entity.setRequestDate(returnRequest.getRequestDate());
        entityManager.persist(entity);
        entityManager.flush();
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Return> findByIdentifier(Return returnRequest) {
        if (returnRequest == null || returnRequest.getIdentifier() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(ReturnJpaEntity.class, returnRequest.getIdentifier()))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Return> findActiveByOrder(Return returnRequest) {
        if (returnRequest == null || returnRequest.getOrder() == null
                || returnRequest.getOrder().getIdentifier() == null) {
            return Optional.empty();
        }
        return entityManager.createQuery(
                        "select r from ReturnJpaEntity r where r.orderIdentifier = :orderIdentifier "
                                + "and r.statusCode in (:activeStatuses)",
                        ReturnJpaEntity.class)
                .setParameter("orderIdentifier", returnRequest.getOrder().getIdentifier())
                .setParameter("activeStatuses",
                        java.util.List.of(ReturnStatus.REQUESTED.getCode(), ReturnStatus.APPROVED.getCode()))
                .getResultStream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Return update(Return returnRequest) {
        if (returnRequest == null || returnRequest.getIdentifier() == null) {
            throw new DomainValidationException("Return identifier is required");
        }
        ReturnJpaEntity entity = entityManager.find(ReturnJpaEntity.class, returnRequest.getIdentifier());
        if (entity == null) {
            throw new DomainValidationException("Return not found");
        }
        entity.setStatusCode(returnRequest.getReturnStatus().getCode());
        entityManager.flush();
        return toDomain(entity);
    }

    private Return toDomain(ReturnJpaEntity entity) {
        Return returnRequest = new Return();
        returnRequest.setIdentifier(entity.getIdentifier());
        returnRequest.setOrder(toOrder(entity.getOrderIdentifier()));
        returnRequest.setReason(entity.getReason());
        returnRequest.setReturnStatus(JpaDomainMapper.returnStatus(entity.getStatusCode()));
        returnRequest.setRequestDate(entity.getRequestDate());
        return returnRequest;
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
}
