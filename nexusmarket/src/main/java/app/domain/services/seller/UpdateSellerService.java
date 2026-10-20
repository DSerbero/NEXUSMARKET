package app.domain.services.seller;

import java.util.ArrayList;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.ports.out.SellerRepositoryPort;
import app.domain.services.DomainValidations;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Update Seller: the seller themselves (or an ADMIN) refreshes profile and
 * email. Email must remain unique across the marketplace.
 */
@Service
@ConditionalOnBean(SellerRepositoryPort.class)
@RequiredArgsConstructor
public class UpdateSellerService {

    private final SellerRepositoryPort sellerRepository;

    public Seller execute(User requestingUser, Seller seller) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(seller, "seller");
        Seller current = sellerRepository.findByIdentifier(seller)
                .orElseThrow(() -> new DomainValidationException("Seller not found"));
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
}