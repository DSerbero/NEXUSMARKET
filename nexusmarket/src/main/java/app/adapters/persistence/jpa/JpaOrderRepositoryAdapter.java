package app.adapters.persistence.jpa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.BuyerJpaEntity;
import app.adapters.persistence.jpa.entities.OrderItemJpaEntity;
import app.adapters.persistence.jpa.entities.OrderJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.DigitalProduct;
import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;
import app.domain.models.PhysicalProduct;
import app.domain.models.Product;
import app.domain.models.Seller;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.valueobjects.ProductType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Order save(Order order) {
        if (order == null || order.getBuyer() == null) {
            throw new DomainValidationException("Order and buyer must be provided");
        }
        if (order.getIdentifier() == null || order.getIdentifier().isBlank()) {
            order.setIdentifier(UUID.randomUUID().toString());
        }
        BuyerJpaEntity buyer = entityManager.find(BuyerJpaEntity.class, order.getBuyer().getIdentifier());
        if (buyer == null) {
            throw new DomainValidationException("Buyer not found");
        }
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setIdentifier(order.getIdentifier());
        entity.setBuyer(buyer);
        entity.setStatusCode(order.getOrderStatus().getCode());
        entity.setCreationDate(order.getCreationDate());
        entity.setCompletionDate(order.getCompletionDate());
        entity.setItems(toEntities(order, entity));
        entityManager.persist(entity);
        entityManager.flush();
        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByIdentifier(Order order) {
        if (order == null || order.getIdentifier() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(OrderJpaEntity.class, order.getIdentifier()))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByBuyer(Order order) {
        if (order == null || order.getBuyer() == null || order.getBuyer().getIdentifier() == null) {
            return List.of();
        }
        return entityManager.createQuery(
                        "select order from OrderJpaEntity order where order.buyer.userIdentifier = :buyerIdentifier "
                                + "order by order.creationDate desc",
                        OrderJpaEntity.class)
                .setParameter("buyerIdentifier", order.getBuyer().getIdentifier())
                .getResultList()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Order update(Order order) {
        if (order == null || order.getIdentifier() == null) {
            throw new DomainValidationException("Order identifier is required");
        }
        OrderJpaEntity entity = entityManager.find(OrderJpaEntity.class, order.getIdentifier());
        if (entity == null) {
            throw new DomainValidationException("Order not found");
        }
        entity.setStatusCode(order.getOrderStatus().getCode());
        entity.setCompletionDate(order.getCompletionDate());
        entityManager.flush();
        return toDomain(entity);
    }

    private List<OrderItemJpaEntity> toEntities(Order order, OrderJpaEntity parent) {
        List<OrderItemJpaEntity> items = new ArrayList<>();
        int position = 0;
        for (OrderItemSnapshot snapshot : order.getItems()) {
            OrderItemJpaEntity item = new OrderItemJpaEntity();
            item.setIdentifier(UUID.randomUUID().toString());
            item.setOrder(parent);
            item.setPosition(position++);
            item.setProductIdentifier(snapshot.getProductIdentifier());
            item.setProductType(snapshot.getProductType().getCode());
            item.setUnitPrice(snapshot.getUnitPrice());
            item.setCurrencyCode(snapshot.getCurrencyCode());
            item.setQuantity(snapshot.getQuantity());
            item.setSellerIdentifier(snapshot.getSellerIdentifier());
            item.setDigitalAssetReference(snapshot.getDigitalAssetReference());
            item.setVariants(new ArrayList<>(snapshot.getVariants()));
            items.add(item);
        }
        return items;
    }

    private Order toDomain(OrderJpaEntity entity) {
        Order order = new Order();
        order.setIdentifier(entity.getIdentifier());
        order.setBuyer(JpaDomainMapper.toBuyer(entity.getBuyer()));
        order.setOrderStatus(JpaDomainMapper.orderStatus(entity.getStatusCode()));
        order.setCreationDate(entity.getCreationDate());
        order.setCompletionDate(entity.getCompletionDate());
        entity.getItems().stream()
                .sorted(Comparator.comparingInt(OrderItemJpaEntity::getPosition))
                .map(this::toSnapshot)
                .forEach(order.getItems()::add);
        return order;
    }

    private OrderItemSnapshot toSnapshot(OrderItemJpaEntity entity) {
        if (!OrderItemSnapshot.CURRENCY_CODE.equals(entity.getCurrencyCode())) {
            throw new DomainValidationException("Unsupported order item currency");
        }
        Product product = JpaDomainMapper.productReference(entity.getProductType(), entity.getProductIdentifier());
        product.setPrice(entity.getUnitPrice());
        product.setVariants(new ArrayList<>(entity.getVariants()));
        if (entity.getSellerIdentifier() != null) {
            Seller seller = new Seller();
            seller.setIdentifier(entity.getSellerIdentifier());
            product.setSeller(seller);
        }
        if (product instanceof DigitalProduct digitalProduct) {
            digitalProduct.setDigitalAsset(entity.getDigitalAssetReference());
        }
        return new OrderItemSnapshot(product, entity.getQuantity());
    }
}