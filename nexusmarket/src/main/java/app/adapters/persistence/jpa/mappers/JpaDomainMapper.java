package app.adapters.persistence.jpa.mappers;

import java.util.ArrayList;

import app.adapters.persistence.jpa.entities.BuyerJpaEntity;
import app.adapters.persistence.jpa.entities.UserJpaEntity;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.DigitalProduct;
import app.domain.models.PhysicalProduct;
import app.domain.models.Product;
import app.domain.models.User;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.ProductType;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

public final class JpaDomainMapper {

    private JpaDomainMapper() {
    }

    public static User toUser(UserJpaEntity entity) {
        User user = new User();
        copyUser(entity, user);
        return user;
    }

    public static Buyer toBuyer(BuyerJpaEntity entity) {
        Buyer buyer = new Buyer();
        copyUser(entity.getUser(), buyer);
        buyer.setPrimaryAddress(entity.getPrimaryAddress());
        buyer.setBuyerStatus(buyerStatus(entity.getBuyerStatusCode()));
        buyer.setAdditionalAddresses(entity.getAdditionalAddresses() == null
                ? new ArrayList<>() : new ArrayList<>(entity.getAdditionalAddresses()));
        return buyer;
    }

    public static UserRole userRole(String code) {
        return switch (code) {
            case "BUYER" -> UserRole.BUYER;
            case "SELLER" -> UserRole.SELLER;
            case "LOGISTICS_OPERATOR" -> UserRole.LOGISTICS_OPERATOR;
            case "ADMIN" -> UserRole.ADMIN;
            case "SUPERVISOR" -> UserRole.SUPERVISOR;
            default -> throw new DomainValidationException("Unsupported persisted user role");
        };
    }

    public static UserStatus userStatus(String code) {
        return switch (code) {
            case "ACTIVE" -> UserStatus.ACTIVE;
            case "BLOCKED" -> UserStatus.BLOCKED;
            case "INACTIVE" -> UserStatus.INACTIVE;
            default -> throw new DomainValidationException("Unsupported persisted user status");
        };
    }

    public static BuyerStatus buyerStatus(String code) {
        return switch (code) {
            case "ENABLED" -> BuyerStatus.ENABLED;
            case "SUSPENDED" -> BuyerStatus.SUSPENDED;
            default -> throw new DomainValidationException("Unsupported persisted buyer status");
        };
    }

    public static OrderStatus orderStatus(String code) {
        return switch (code) {
            case "PENDING_PAYMENT" -> OrderStatus.PENDING_PAYMENT;
            case "PAID" -> OrderStatus.PAID;
            case "DISPATCHED" -> OrderStatus.DISPATCHED;
            case "DELIVERED_FINALIZED" -> OrderStatus.DELIVERED_FINALIZED;
            default -> throw new DomainValidationException("Unsupported persisted order status");
        };
    }

    public static Product productReference(String code, String identifier) {
        Product product;
        if (ProductType.PHYSICAL.getCode().equals(code)) {
            product = new PhysicalProduct();
        } else if (ProductType.DIGITAL.getCode().equals(code)) {
            product = new DigitalProduct();
        } else {
            throw new DomainValidationException("Unsupported persisted product type");
        }
        product.setIdentifier(identifier);
        return product;
    }

    private static void copyUser(UserJpaEntity entity, User user) {
        user.setIdentifier(entity.getIdentifier());
        user.setFullName(entity.getFullName());
        user.setEmail(entity.getNormalizedEmail());
        user.setRole(userRole(entity.getRoleCode()));
        user.setStatus(userStatus(entity.getStatusCode()));
    }
}