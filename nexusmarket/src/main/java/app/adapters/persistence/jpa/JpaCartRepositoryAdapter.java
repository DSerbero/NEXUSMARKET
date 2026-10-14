package app.adapters.persistence.jpa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import app.adapters.persistence.jpa.entities.BuyerJpaEntity;
import app.adapters.persistence.jpa.entities.CartItemJpaEntity;
import app.adapters.persistence.jpa.entities.CartJpaEntity;
import app.adapters.persistence.jpa.mappers.JpaDomainMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.CartItem;
import app.domain.models.Product;
import app.domain.ports.out.CartRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaCartRepositoryAdapter implements CartRepositoryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Cart save(Cart cart) {
        return store(cart, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cart> findByBuyer(Cart cart) {
        String buyerIdentifier = buyerIdentifier(cart);
        if (buyerIdentifier == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(CartJpaEntity.class, buyerIdentifier))
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<Cart> findByBuyerForUpdate(Cart cart) {
        String buyerIdentifier = buyerIdentifier(cart);
        if (buyerIdentifier == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entityManager.find(
                        CartJpaEntity.class, buyerIdentifier, LockModeType.PESSIMISTIC_WRITE))
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Cart update(Cart cart) {
        return store(cart, true);
    }

    private Cart store(Cart cart, boolean mustExist) {
        String buyerIdentifier = buyerIdentifier(cart);
        if (buyerIdentifier == null) {
            throw new DomainValidationException("Cart buyer identifier is required");
        }
        CartJpaEntity entity = entityManager.find(CartJpaEntity.class, buyerIdentifier);
        if (entity == null && mustExist) {
            throw new DomainValidationException("Cart not found");
        }
        if (entity == null) {
            BuyerJpaEntity buyer = entityManager.find(BuyerJpaEntity.class, buyerIdentifier);
            if (buyer == null) {
                throw new DomainValidationException("Buyer not found");
            }
            entity = new CartJpaEntity();
            entity.setBuyerIdentifier(buyerIdentifier);
            entity.setBuyer(buyer);
            entity.setCreationDate(cart.getCreationDate());
            entityManager.persist(entity);
        } else if (cart.getCreationDate() != null) {
            entity.setCreationDate(cart.getCreationDate());
        }
        entity.getItems().clear();
        int position = 0;
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            CartItemJpaEntity itemEntity = new CartItemJpaEntity();
            itemEntity.setIdentifier(UUID.randomUUID().toString());
            itemEntity.setCart(entity);
            itemEntity.setPosition(position++);
            itemEntity.setProductIdentifier(product.getIdentifier());
            itemEntity.setProductType(productTypeCode(product));
            itemEntity.setQuantity(item.getQuantity());
            entity.getItems().add(itemEntity);
        }
        entityManager.flush();
        return toDomain(entity);
    }

    private Cart toDomain(CartJpaEntity entity) {
        Cart cart = new Cart();
        cart.setBuyer(JpaDomainMapper.toBuyer(entity.getBuyer()));
        cart.setCreationDate(entity.getCreationDate());
        entity.getItems().stream()
                .sorted(Comparator.comparingInt(CartItemJpaEntity::getPosition))
                .map(item -> {
                    Product product = JpaDomainMapper.productReference(item.getProductType(), item.getProductIdentifier());
                    return new CartItem(product, item.getQuantity());
                })
                .forEach(cart.getItems()::add);
        return cart;
    }

    private String buyerIdentifier(Cart cart) {
        return cart == null || cart.getBuyer() == null ? null : cart.getBuyer().getIdentifier();
    }

    private String productTypeCode(Product product) {
        if (product instanceof app.domain.models.DigitalProduct) {
            return app.domain.valueobjects.ProductType.DIGITAL.getCode();
        }
        if (product instanceof app.domain.models.PhysicalProduct) {
            return app.domain.valueobjects.ProductType.PHYSICAL.getCode();
        }
        throw new DomainValidationException("Unsupported cart product type");
    }
}