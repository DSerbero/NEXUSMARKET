package app.domain.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.services.ports.SellerRepositoryPort;
import app.domain.services.ports.UserRepositoryPort;
import app.domain.valueObjects.UserRole;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(SellerRepositoryPort.class)
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepositoryPort sellerRepository;
    private final UserRepositoryPort userRepository;

    public Seller register(User requestingUser, Seller seller) {
        DomainValidations.active(requestingUser);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new app.domain.services.exceptions.DomainValidationException("Only an admin can register sellers");
        }
        DomainValidations.required(seller, "seller");
        DomainValidations.requiredText(seller.getFullName(), "fullName");
        DomainValidations.requiredText(seller.getEmail(), "email");
        if (sellerRepository.existsByEmail(seller)) {
            throw new app.domain.services.exceptions.DomainValidationException("Email already exists");
        }
        seller.setRole(UserRole.SELLER);
        userRepository.save(seller);
        return sellerRepository.save(seller);
    }

    public Seller consult(User requestingUser, Seller seller) {
        DomainValidations.active(requestingUser);
        return sellerRepository.findByIdentifier(seller).orElseThrow();
    }

    public Seller update(User requestingUser, Seller seller) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(seller, "seller");
        return sellerRepository.update(seller);
    }
}
