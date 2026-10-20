package app.domain.services.buyer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.services.DomainValidations;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import lombok.RequiredArgsConstructor;

/**
 * Register Buyer: creates a commercially enabled buyer account. Email must be
 * unique across the marketplace.
 */
@Service
@ConditionalOnBean(BuyerRepositoryPort.class)
@RequiredArgsConstructor
public class RegisterBuyerService {

    private final BuyerRepositoryPort buyerRepository;

    public Buyer execute(Buyer buyer) {
        DomainValidations.required(buyer, "buyer");
        DomainValidations.requiredText(buyer.getFullName(), "fullName");
        DomainValidations.requiredText(buyer.getEmail(), "email");
        DomainValidations.requiredText(buyer.getPrimaryAddress(), "primaryAddress");
        if (buyerRepository.existsByEmail(buyer)) {
            throw new DomainValidationException("Email already exists");
        }
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setBuyerStatus(BuyerStatus.ENABLED);
        return buyerRepository.save(buyer);
    }
}