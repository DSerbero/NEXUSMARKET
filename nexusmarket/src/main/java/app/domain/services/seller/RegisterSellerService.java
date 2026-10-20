package app.domain.services.seller;

import app.domain.services.DomainValidations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.exceptions.DomainValidationException;
import app.domain.ports.out.SellerRepositoryPort;
import app.domain.ports.out.UserRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import lombok.RequiredArgsConstructor;

/**
 * Register Seller: only an ADMIN can create a seller profile and its initial
 * warehouse. Both the seller row and the warehouse row are persisted as part of
 * the same operation so the ownership is consistent from the start.
 */
@Service
@ConditionalOnBean(SellerRepositoryPort.class)
@RequiredArgsConstructor
public class RegisterSellerService {

    private final SellerRepositoryPort sellerRepository;
    private final UserRepositoryPort userRepository;
    private final WarehouseRepositoryPort warehouseRepository;

    public Seller execute(User requestingUser, Seller seller, Warehouse initialWarehouse) {
        DomainValidations.active(requestingUser);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("Only an admin can register sellers");
        }
        DomainValidations.required(seller, "seller");
        DomainValidations.requiredText(seller.getFullName(), "fullName");
        DomainValidations.requiredText(seller.getEmail(), "email");
        if (sellerRepository.existsByEmail(seller) || userRepository.existsByEmail(seller)) {
            throw new DomainValidationException("Email already exists");
        }
        DomainValidations.required(initialWarehouse, "initialWarehouse");
        DomainValidations.requiredText(initialWarehouse.getLocation(), "location");
        seller.setRole(UserRole.SELLER);
        seller.setStatus(UserStatus.ACTIVE);
        Seller savedSeller = sellerRepository.save(seller);
        initialWarehouse.setOwnerType(app.domain.valueobjects.WarehouseOwnerType.SELLER);
        initialWarehouse.setOwner(savedSeller);
        initialWarehouse.setResponsibleUser(savedSeller);
        warehouseRepository.save(initialWarehouse);
        return savedSeller;
    }
}