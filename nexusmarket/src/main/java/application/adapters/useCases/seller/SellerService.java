package application.adapters.useCases.seller;

import application.domain.services.DomainValidations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.exceptions.DomainValidationException;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(SellerRepositoryPort.class)
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepositoryPort sellerRepository;
    private final UserRepositoryPort userRepository;
    private final WarehouseRepositoryPort warehouseRepository;

    public Seller register(User requestingUser, Seller seller, Warehouse initialWarehouse) {
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
        initialWarehouse.setOwnerType(application.domain.valueobjects.WarehouseOwnerType.SELLER);
        initialWarehouse.setOwner(savedSeller);
        initialWarehouse.setResponsibleUser(savedSeller);
        warehouseRepository.save(initialWarehouse);
        return savedSeller;
    }

    public Seller consult(User requestingUser, Seller seller) {
        DomainValidations.active(requestingUser);
        Seller current = authoritative(seller);
        if (!DomainValidations.sameIdentity(requestingUser, current)
                && !UserRole.ADMIN.equals(requestingUser.getRole())
                && !UserRole.SUPERVISOR.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot consult this seller");
        }
        return current;
    }

    public Seller update(User requestingUser, Seller seller) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(seller, "seller");
        Seller current = authoritative(seller);
        if (!DomainValidations.sameIdentity(requestingUser, current)
                && !UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot update this seller");
        }
        DomainValidations.requiredText(seller.getFullName(), "fullName");
        DomainValidations.requiredText(seller.getEmail(), "email");
        Seller emailQuery = new Seller();
        emailQuery.setEmail(seller.getEmail());
        sellerRepository.findByEmail(emailQuery).ifPresent(existing -> {
            if (!DomainValidations.sameIdentity(existing, current)) {
                throw new DomainValidationException("Email already exists");
            }
        });
        current.setFullName(seller.getFullName());
        current.setEmail(seller.getEmail());
        return sellerRepository.update(current);
    }

    private Seller authoritative(Seller seller) {
        DomainValidations.required(seller, "seller");
        return sellerRepository.findByIdentifier(seller)
                .orElseThrow(() -> new DomainValidationException("Seller not found"));
    }
}
