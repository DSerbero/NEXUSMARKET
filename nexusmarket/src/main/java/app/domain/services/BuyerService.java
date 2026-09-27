package app.domain.services;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Buyer;
import app.domain.services.ports.BuyerRepositoryPort;
import app.domain.valueObjects.BuyerStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(BuyerRepositoryPort.class)
@RequiredArgsConstructor
public class BuyerService {

    private final BuyerRepositoryPort buyerRepository;

    public Buyer register(Buyer buyer) {
        DomainValidations.required(buyer, "buyer");
        DomainValidations.requiredText(buyer.getFullName(), "fullName");
        DomainValidations.requiredText(buyer.getEmail(), "email");
        DomainValidations.requiredText(buyer.getPrimaryAddress(), "primaryAddress");
        if (buyerRepository.existsByEmail(buyer)) {
            throw new app.domain.services.exceptions.DomainValidationException("Email already exists");
        }
        if (buyer.getBuyerStatus() == null) {
            buyer.setBuyerStatus(BuyerStatus.ENABLED);
        }
        return buyerRepository.save(buyer);
    }

    public Buyer consult(Buyer requestingBuyer, Buyer targetBuyer) {
        DomainValidations.active(requestingBuyer);
        return buyerRepository.findByIdentifier(targetBuyer).orElseThrow();
    }

    public Buyer update(Buyer requestingBuyer, Buyer buyer) {
        DomainValidations.active(requestingBuyer);
        DomainValidations.required(buyer, "buyer");
        DomainValidations.requiredText(buyer.getPrimaryAddress(), "primaryAddress");
        return buyerRepository.update(buyer);
    }

    public Buyer changeCommercialStatus(Buyer requestingBuyer, Buyer buyer, BuyerStatus status) {
        DomainValidations.active(requestingUser(requestingBuyer));
        buyer.setBuyerStatus(DomainValidations.required(status, "status"));
        return buyerRepository.update(buyer);
    }

    private Buyer requestingUser(Buyer buyer) {
        return DomainValidations.required(buyer, "requestingBuyer");
    }
}
