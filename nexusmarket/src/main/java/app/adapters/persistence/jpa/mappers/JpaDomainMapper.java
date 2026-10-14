package app.adapters.persistence.jpa.mappers;

import java.util.ArrayList;

import app.adapters.persistence.jpa.entities.BuyerJpaEntity;
import app.adapters.persistence.jpa.entities.SellerJpaEntity;
import app.adapters.persistence.jpa.entities.UserJpaEntity;
import app.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.DigitalProduct;
import app.domain.models.PhysicalProduct;
import app.domain.models.Product;
import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.InvoiceStatus;
import app.domain.valueobjects.MovementType;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.ProductType;
import app.domain.valueobjects.RefundStatus;
import app.domain.valueobjects.ReturnStatus;
import app.domain.valueobjects.ShipmentStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import app.domain.valueobjects.WarehouseOwnerType;

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

    public static Seller toSeller(SellerJpaEntity entity) {
        Seller seller = new Seller();
        copyUser(entity.getUser(), seller);
        return seller;
    }

    public static Warehouse toWarehouse(WarehouseJpaEntity entity, UserJpaEntity owner, UserJpaEntity responsible) {
        Warehouse warehouse = new Warehouse();
        warehouse.setIdentifier(entity.getIdentifier());
        warehouse.setLocation(entity.getLocation());
        warehouse.setOwnerType(warehouseOwnerType(entity.getOwnerType()));
        if (owner != null) {
            User ownerUser = new User();
            copyUser(owner, ownerUser);
            warehouse.setOwner(ownerUser);
        }
        if (responsible != null) {
            User responsibleUser = new User();
            copyUser(responsible, responsibleUser);
            warehouse.setResponsibleUser(responsibleUser);
        }
        return warehouse;
    }

    public static WarehouseOwnerType warehouseOwnerType(String code) {
        return switch (code) {
            case "MARKETPLACE" -> WarehouseOwnerType.MARKETPLACE;
            case "SELLER" -> WarehouseOwnerType.SELLER;
            default -> throw new DomainValidationException("Unsupported persisted warehouse owner type");
        };
    }

    public static MovementType movementType(String code) {
        return switch (code) {
            case "STOCK_IN" -> MovementType.STOCK_IN;
            case "RESERVATION" -> MovementType.RESERVATION;
            case "SALE_OUTBOUND" -> MovementType.SALE_OUTBOUND;
            case "ADJUSTMENT" -> MovementType.ADJUSTMENT;
            case "RETURN_INBOUND" -> MovementType.RETURN_INBOUND;
            default -> throw new DomainValidationException("Unsupported persisted movement type");
        };
    }

    public static InvoiceStatus invoiceStatus(String code) {
        return switch (code) {
            case "ISSUED" -> InvoiceStatus.ISSUED;
            case "PAID" -> InvoiceStatus.PAID;
            case "VOIDED" -> InvoiceStatus.VOIDED;
            default -> throw new DomainValidationException("Unsupported persisted invoice status");
        };
    }

    public static ShipmentStatus shipmentStatus(String code) {
        return switch (code) {
            case "IN_PREPARATION" -> ShipmentStatus.IN_PREPARATION;
            case "DISPATCHED" -> ShipmentStatus.DISPATCHED;
            case "IN_TRANSIT" -> ShipmentStatus.IN_TRANSIT;
            case "DELIVERED" -> ShipmentStatus.DELIVERED;
            default -> throw new DomainValidationException("Unsupported persisted shipment status");
        };
    }

    public static ReturnStatus returnStatus(String code) {
        return switch (code) {
            case "REQUESTED" -> ReturnStatus.REQUESTED;
            case "APPROVED" -> ReturnStatus.APPROVED;
            case "REJECTED" -> ReturnStatus.REJECTED;
            case "COMPLETED" -> ReturnStatus.COMPLETED;
            default -> throw new DomainValidationException("Unsupported persisted return status");
        };
    }

    public static RefundStatus refundStatus(String code) {
        return switch (code) {
            case "PENDING" -> RefundStatus.PENDING;
            case "PROCESSED" -> RefundStatus.PROCESSED;
            case "REJECTED" -> RefundStatus.REJECTED;
            default -> throw new DomainValidationException("Unsupported persisted refund status");
        };
    }

    private static void copyUser(UserJpaEntity entity, User user) {
        user.setIdentifier(entity.getIdentifier());
        user.setFullName(entity.getFullName());
        user.setEmail(entity.getNormalizedEmail());
        user.setRole(userRole(entity.getRoleCode()));
        user.setStatus(userStatus(entity.getStatusCode()));
    }
}