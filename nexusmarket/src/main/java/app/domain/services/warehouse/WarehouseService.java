package app.domain.services.warehouse;

import app.domain.services.DomainValidations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.LogisticOperator;
import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.exceptions.DomainValidationException;
import app.domain.ports.out.SellerRepositoryPort;
import app.domain.ports.out.UserRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.WarehouseOwnerType;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(WarehouseRepositoryPort.class)
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepositoryPort warehouseRepository;
    private final UserRepositoryPort userRepository;
    private final SellerRepositoryPort sellerRepository;

    public Warehouse register(User requestingUser, Warehouse warehouse) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(warehouse, "warehouse");
        DomainValidations.requiredText(warehouse.getLocation(), "location");
        validateOwner(requestingUser, warehouse);
        return warehouseRepository.save(warehouse);
    }

    public Warehouse consult(User requestingUser, Warehouse warehouse) {
        DomainValidations.active(requestingUser);
        Warehouse current = authoritative(warehouse);
        boolean allowed = UserRole.ADMIN.equals(requestingUser.getRole())
                || UserRole.SUPERVISOR.equals(requestingUser.getRole())
                || (UserRole.SELLER.equals(requestingUser.getRole())
                        && DomainValidations.sameIdentity(requestingUser, current.getOwner()))
                || (UserRole.LOGISTICS_OPERATOR.equals(requestingUser.getRole())
                        && DomainValidations.sameIdentity(requestingUser, current.getResponsibleUser()));
        if (!allowed) {
            throw new DomainValidationException("User cannot consult this warehouse");
        }
        return current;
    }

    public Warehouse assignLogisticsOperator(User requestingUser, Warehouse warehouse,
            LogisticOperator operator) {
        DomainValidations.active(requestingUser);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("Only an admin can assign logistics operators");
        }
        Warehouse current = authoritative(warehouse);
        DomainValidations.required(operator, "operator");
        DomainValidations.active(operator);
        if (!WarehouseOwnerType.MARKETPLACE.equals(current.getOwnerType())
                || !UserRole.LOGISTICS_OPERATOR.equals(operator.getRole())) {
            throw new DomainValidationException(
                    "Only marketplace warehouses can have a logistics operator");
        }
        User persistedOperator = userRepository.findByIdentifier(operator)
                .orElseThrow(() -> new DomainValidationException("Logistics operator not found"));
        DomainValidations.active(persistedOperator);
        if (!UserRole.LOGISTICS_OPERATOR.equals(persistedOperator.getRole())) {
            throw new DomainValidationException("Assigned user must be a logistics operator");
        }
        current.setResponsibleUser(persistedOperator);
        return warehouseRepository.update(current);
    }

    public Warehouse update(User requestingUser, Warehouse warehouse) {
        DomainValidations.active(requestingUser);
        Warehouse current = authoritative(warehouse);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())
                && !DomainValidations.sameIdentity(requestingUser, current.getOwner())) {
            throw new DomainValidationException("User cannot update this warehouse");
        }
        DomainValidations.requiredText(warehouse.getLocation(), "location");
        current.setLocation(warehouse.getLocation());
        return warehouseRepository.update(current);
    }

    private void validateOwner(User requestingUser, Warehouse warehouse) {
        DomainValidations.required(warehouse.getOwnerType(), "ownerType");
        User owner = DomainValidations.required(warehouse.getOwner(), "owner");
        if (WarehouseOwnerType.MARKETPLACE.equals(warehouse.getOwnerType())) {
            if (!UserRole.ADMIN.equals(requestingUser.getRole())
                    || !DomainValidations.sameIdentity(requestingUser, owner)
                    || !UserRole.ADMIN.equals(owner.getRole())) {
                throw new DomainValidationException("Only the requesting admin can create a marketplace warehouse");
            }
            User persistedOwner = userRepository.findByIdentifier(owner)
                    .orElseThrow(() -> new DomainValidationException("Warehouse owner not found"));
            DomainValidations.active(persistedOwner);
            if (warehouse.getResponsibleUser() != null) {
                validateResponsibleOperator(warehouse.getResponsibleUser());
            }
        } else if (WarehouseOwnerType.SELLER.equals(warehouse.getOwnerType())) {
            if (!(owner instanceof Seller) || !UserRole.SELLER.equals(requestingUser.getRole())
                    || !DomainValidations.sameIdentity(requestingUser, owner)) {
                throw new DomainValidationException("Only a seller can create its own warehouse");
            }
            Seller persistedSeller = sellerRepository.findByIdentifier((Seller) owner)
                    .orElseThrow(() -> new DomainValidationException("Seller owner not found"));
            DomainValidations.active(persistedSeller);
            warehouse.setOwner(persistedSeller);
            warehouse.setResponsibleUser(persistedSeller);
        } else {
            throw new DomainValidationException("Unsupported warehouse owner type");
        }
    }

    private Warehouse authoritative(Warehouse warehouse) {
        DomainValidations.required(warehouse, "warehouse");
        return warehouseRepository.findByIdentifier(warehouse)
                .orElseThrow(() -> new DomainValidationException("Warehouse not found"));
    }

    private void validateResponsibleOperator(User operator) {
        DomainValidations.active(operator);
        if (!UserRole.LOGISTICS_OPERATOR.equals(operator.getRole())) {
            throw new DomainValidationException("Responsible user must be a logistics operator");
        }
    }
}
