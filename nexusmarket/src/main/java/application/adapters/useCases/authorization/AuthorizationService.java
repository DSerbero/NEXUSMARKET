package application.adapters.useCases.authorization;

import application.domain.services.DomainValidations;

import org.springframework.stereotype.Service;

import application.domain.models.Cart;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Return;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.exceptions.DomainValidationException;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;

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
        if (!user.equals(shipment.getOperator()) && !UserRole.ADMIN.equals(user.getRole())) {
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
