package app.adapters.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.RefundJpaEntity;
import app.adapters.persistence.jpa.entities.ReturnJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Refund;
import app.domain.models.Return;
import app.domain.ports.out.RefundRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persists {@link Refund}. The originating return is referenced by identifier and
 * rehydrated as a domain {@link Return}.
 */
@Repository
public class JpaRefundRepositoryAdapter implements RefundRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Refund save(Refund refund) {
        DomainValidationsAdapter.require(refund, "refund");
        DomainValidationsAdapter.require(refund.getRelatedReturn(), "relatedReturn");
        DomainValidationsAdapter.require(refund.getRefundedAmount(), "refundedAmount");
        if (refund.getIdentifier() == null || refund.getIdentifier().isBlank()) {
            refund.setIdentifier(UUID.randomUUID().toString());
        }
        ReturnJpaEntity relatedReturn = entityManager.find(
                ReturnJpaEntity.class, refund.getRelatedReturn().getIdentifier());
        if (relatedReturn == null) {
            throw new DomainValidationException("Return not found");
        }
        RefundJpaEntity entity = new RefundJpaEntity();
        entity.setIdentifier(refund.getIdentifier());
        entity.setReturnIdentifier(relatedReturn.getIdentifier());
        entity.setRefundedAmount(refund.getRefundedAmount());
        entity.setCurrencyCode(refund.getCurrencyCode());
        entity.setStatusCode(refund.getRefundStatus().getCode());
        entity.setProcessingDate(refund.getProcessingDate());
        entityManager.persist(entity);
        entityManager.flush();
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Refund> findByReturn(Refund refund) {
        if (refund == null || refund.getRelatedReturn() == null
                || refund.getRelatedReturn().getIdentifier() == null) {
            return Optional.empty();
        }
        return entityManager.createQuery(
                        "select r from RefundJpaEntity r where r.returnIdentifier = :returnIdentifier",
                        RefundJpaEntity.class)
                .setParameter("returnIdentifier", refund.getRelatedReturn().getIdentifier())
                .getResultStream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Refund> findByIdentifier(Refund refund) {
        if (refund == null || refund.getIdentifier() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(RefundJpaEntity.class, refund.getIdentifier()))
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Refund update(Refund refund) {
        if (refund == null || refund.getIdentifier() == null) {
            throw new DomainValidationException("Refund identifier is required");
        }
        RefundJpaEntity entity = entityManager.find(RefundJpaEntity.class, refund.getIdentifier());
        if (entity == null) {
            throw new DomainValidationException("Refund not found");
        }
        entity.setStatusCode(refund.getRefundStatus().getCode());
        entityManager.flush();
        return toDomain(entity);
    }

    private Refund toDomain(RefundJpaEntity entity) {
        Refund refund = new Refund();
        refund.setIdentifier(entity.getIdentifier());
        refund.setRelatedReturn(toReturn(entity.getReturnIdentifier()));
        refund.setRefundedAmount(entity.getRefundedAmount());
        refund.setCurrencyCode(entity.getCurrencyCode());
        refund.setRefundStatus(JpaDomainMapper.refundStatus(entity.getStatusCode()));
        refund.setProcessingDate(entity.getProcessingDate());
        return refund;
    }

    private Return toReturn(String returnIdentifier) {
        ReturnJpaEntity relatedReturn = entityManager.find(ReturnJpaEntity.class, returnIdentifier);
        if (relatedReturn == null) {
            throw new DomainValidationException("Return not found");
        }
        Return domain = new Return();
        domain.setIdentifier(relatedReturn.getIdentifier());
        domain.setReturnStatus(JpaDomainMapper.returnStatus(relatedReturn.getStatusCode()));
        return domain;
    }
}
