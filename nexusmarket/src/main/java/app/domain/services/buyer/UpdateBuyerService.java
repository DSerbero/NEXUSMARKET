package app.domain.services.buyer;

import java.util.ArrayList;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.User;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.services.DomainValidations;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Update Buyer: the buyer themselves (or an ADMIN) refreshes contact data and
 * addresses. Email stays unique across the marketplace.
 */
@Service
@ConditionalOnBean(BuyerRepositoryPort.class)
@RequiredArgsConstructor
public class UpdateBuyerService {

    private final BuyerRepositoryPort buyerRepository;

    public Buyer execute(User requestingUser, Buyer buyer) {
        DomainValidations.active(requestingUser);
        Buyer current = buyerRepository.findByIdentifier(buyer)
                .orElseThrow(() -> new DomainValidationException("Buyer not found"));
        if (!DomainValidations.sameIdentity(requestingUser, current)
                && !UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot update this buyer");
        }
        DomainValidations.requiredText(buyer.getFullName(), "fullName");
        DomainValidations.requiredText(buyer.getEmail(), "email");
        DomainValidations.requiredText(buyer.getPrimaryAddress(), "primaryAddress");
        Buyer emailQuery = new Buyer();
        emailQuery.setEmail(buyer.getEmail());
        buyerRepository.findByEmail(emailQuery).ifPresent(existing -> {
            if (!DomainValidations.sameIdentity(existing, current)) {
                throw new DomainValidationException("Email already exists");
            }
        });
        current.setFullName(buyer.getFullName());
        current.setEmail(buyer.getEmail());
        current.setPrimaryAddress(buyer.getPrimaryAddress());
        current.setAdditionalAddresses(buyer.getAdditionalAddresses() == null
                ? new ArrayList<>() : new ArrayList<>(buyer.getAdditionalAddresses()));
        return buyerRepository.update(current);
    }
}