package app.adapters.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import app.adapters.persistence.jpa.entities.OrderJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Invoice;
import app.domain.models.Order;
import app.domain.ports.out.InvoiceRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persists {@link Invoice}. The order is referenced logically by identifier and
 * rehydrated as a domain {@link Order} so refund processing can read it.
 */
@Repository
public class JpaInvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Invoice save(Invoice invoice) {
        DomainValidationsAdapter.require(invoice, "invoice");
        DomainValidationsAdapter.require(invoice.getOrder(), "order");
        DomainValidationsAdapter.require(invoice.getTotalAmount(), "totalAmount");
        if (invoice.getIdentifier() == null || invoice.getIdentifier().isBlank()) {
            invoice.setIdentifier(UUID.randomUUID().toString());
        }
        OrderJpaEntity order = entityManager.find(OrderJpaEntity.class, invoice.getOrder().getIdentifier());
        if (order == null) {
            throw new DomainValidationException("Order not found");
        }
        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setIdentifier(invoice.getIdentifier());
        entity.setOrderIdentifier(order.getIdentifier());
        entity.setTotalAmount(invoice.getTotalAmount());
        entity.setCurrencyCode(invoice.getCurrencyCode());
        entity.setIssueDate(invoice.getIssueDate());
        entity.setStatusCode(invoice.getInvoiceStatus().getCode());
        entityManager.persist(entity);
        entityManager.flush();
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findByOrder(Invoice invoice) {
        if (invoice == null || invoice.getOrder() == null
                || invoice.getOrder().getIdentifier() == null) {
            return Optional.empty();
        }
        return entityManager.createQuery(
                        "select i from InvoiceJpaEntity i where i.orderIdentifier = :orderIdentifier",
                        InvoiceJpaEntity.class)
                .setParameter("orderIdentifier", invoice.getOrder().getIdentifier())
                .getResultStream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findByIdentifier(Invoice invoice) {
        if (invoice == null || invoice.getIdentifier() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(InvoiceJpaEntity.class, invoice.getIdentifier()))
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Invoice update(Invoice invoice) {
        if (invoice == null || invoice.getIdentifier() == null) {
            throw new DomainValidationException("Invoice identifier is required");
        }
        InvoiceJpaEntity entity = entityManager.find(InvoiceJpaEntity.class, invoice.getIdentifier());
        if (entity == null) {
            throw new DomainValidationException("Invoice not found");
        }
        entity.setStatusCode(invoice.getInvoiceStatus().getCode());
        entityManager.flush();
        return toDomain(entity);
    }

    private Invoice toDomain(InvoiceJpaEntity entity) {
        Invoice invoice = new Invoice();
        invoice.setIdentifier(entity.getIdentifier());
        invoice.setOrder(toOrder(entity.getOrderIdentifier()));
        invoice.setTotalAmount(entity.getTotalAmount());
        invoice.setCurrencyCode(entity.getCurrencyCode());
        invoice.setIssueDate(entity.getIssueDate());
        invoice.setInvoiceStatus(JpaDomainMapper.invoiceStatus(entity.getStatusCode()));
        return invoice;
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
