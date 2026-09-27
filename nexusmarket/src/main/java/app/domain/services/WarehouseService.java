package app.domain.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.LogisticOperator;
import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.services.ports.SellerRepositoryPort;
import app.domain.services.ports.UserRepositoryPort;
import app.domain.services.ports.WarehouseRepositoryPort;
import app.domain.valueObjects.UserRole;
import app.domain.valueObjects.WarehouseOwnerType;
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
        validateOwner(warehouse);
        return warehouseRepository.save(warehouse);
    }

    public Warehouse consult(User requestingUser, Warehouse warehouse) {
        DomainValidations.active(requestingUser);
        return warehouseRepository.findByIdentifier(warehouse).orElseThrow();
    }

    public Warehouse assignLogisticsOperator(User requestingUser, Warehouse warehouse,
            LogisticOperator operator) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(warehouse, "warehouse");
        DomainValidations.required(operator, "operator");
        if (!WarehouseOwnerType.MARKETPLACE.equals(warehouse.getOwnerType())
                || !UserRole.LOGISTICS_OPERATOR.equals(operator.getRole())) {
            throw new app.domain.services.exceptions.DomainValidationException(
                    "Only marketplace warehouses can have a logistics operator");
        }
        warehouse.setResponsibleUser(operator);
        operator.setAssignedWarehouse(warehouse);
        return warehouseRepository.update(warehouse);
    }

    public Warehouse update(User requestingUser, Warehouse warehouse) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(warehouse, "warehouse");
        DomainValidations.requiredText(warehouse.getLocation(), "location");
        return warehouseRepository.update(warehouse);
    }

    private void validateOwner(Warehouse warehouse) {
        DomainValidations.required(warehouse.getOwnerType(), "ownerType");
        User owner = DomainValidations.required(warehouse.getOwner(), "owner");
        if (WarehouseOwnerType.MARKETPLACE.equals(warehouse.getOwnerType())) {
            if (!UserRole.ADMIN.equals(owner.getRole())) {
                throw new app.domain.services.exceptions.DomainValidationException("Marketplace owner must be an admin");
            }
            userRepository.findByIdentifier(owner).orElseThrow();
        } else if (WarehouseOwnerType.SELLER.equals(warehouse.getOwnerType())) {
            if (!(owner instanceof Seller)) {
                throw new app.domain.services.exceptions.DomainValidationException("Seller warehouse owner is invalid");
            }
            sellerRepository.findByIdentifier((Seller) owner).orElseThrow();
            warehouse.setResponsibleUser(owner);
        }
    }
}
