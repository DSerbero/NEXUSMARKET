package app.domain.services;

import org.springframework.stereotype.Service;

import app.domain.models.Cart;
import app.domain.models.Order;
import app.domain.models.Product;
import app.domain.models.Return;
import app.domain.models.Shipment;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.services.exceptions.DomainValidationException;
import app.domain.valueObjects.UserRole;
import app.domain.valueObjects.UserStatus;

@Service
public class AuthorizationService {

    public void requireActive(User user) {
        DomainValidations.active(user);
    }

    public void requireRole(User user, UserRole role) {
        DomainValidations.active(user);
        if (!role.equals(user.getRole())) {
            throw new DomainValidationException("User does not have the required role");
        }
    }

    public void authorizeCart(User user, Cart cart) {
        requireRole(user, UserRole.BUYER);
        if (!user.equals(cart.getBuyer())) {
            throw new DomainValidationException("Buyer does not own cart");
        }
    }

    public void authorizeOrderRead(User user, Order order) {
        DomainValidations.active(user);
        boolean allowed = UserRole.ADMIN.equals(user.getRole())
                || UserRole.SUPERVISOR.equals(user.getRole())
                || user.equals(order.getBuyer());
        if (!allowed) {
            throw new DomainValidationException("User cannot consult order");
        }
    }

    public void authorizeProductWrite(User user, Product product) {
        requireRole(user, UserRole.SELLER);
        if (!user.equals(product.getSeller())) {
            throw new DomainValidationException("Seller does not own product");
        }
    }

    public void authorizeWarehouse(User user, Warehouse warehouse) {
        DomainValidations.active(user);
        boolean allowed = UserRole.ADMIN.equals(user.getRole())
                || user.equals(warehouse.getOwner())
                || user.equals(warehouse.getResponsibleUser());
        if (!allowed) {
            throw new DomainValidationException("User cannot operate warehouse");
        }
    }

    public void authorizeShipment(User user, Shipment shipment) {
        DomainValidations.active(user);
        if (!user.equals(shipment.getOperator()) && !UserRole.ADMIN.equals(user.getRole())
                && !UserRole.SUPERVISOR.equals(user.getRole())) {
            throw new DomainValidationException("User cannot operate shipment");
        }
    }

    public void authorizeReturn(User user, Return returnRequest) {
        DomainValidations.active(user);
        boolean allowed = UserRole.ADMIN.equals(user.getRole())
                || user.equals(returnRequest.getOrder().getBuyer());
        if (!allowed) {
            throw new DomainValidationException("User cannot operate return");
        }
    }

    public boolean isInactive(User user) {
        return user == null || !UserStatus.ACTIVE.equals(user.getStatus());
    }
}
