package app.domain.services.buyer;

import app.domain.services.DomainValidations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Buyer;
import app.domain.models.User;
import app.domain.exceptions.DomainValidationException;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

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
            throw new DomainValidationException("Email already exists");
        }
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setBuyerStatus(BuyerStatus.ENABLED);
        return buyerRepository.save(buyer);
    }

    public Buyer consult(User requestingUser, Buyer targetBuyer) {
        DomainValidations.active(requestingUser);
        Buyer current = authoritative(targetBuyer);
        if (!DomainValidations.sameIdentity(requestingUser, current)
                && !UserRole.ADMIN.equals(requestingUser.getRole())
                && !UserRole.SUPERVISOR.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot consult this buyer");
        }
        return current;
    }

    public Buyer update(User requestingUser, Buyer buyer) {
        DomainValidations.active(requestingUser);
        Buyer current = authoritative(buyer);
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

    public Buyer changeCommercialStatus(User requestingUser, Buyer buyer, BuyerStatus status) {
        DomainValidations.active(requestingUser);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("Only an admin can change buyer commercial status");
        }
        Buyer current = authoritative(buyer);
        BuyerStatus nextStatus = DomainValidations.required(status, "status");
        boolean validTransition = (BuyerStatus.ENABLED.equals(current.getBuyerStatus())
                        && BuyerStatus.SUSPENDED.equals(nextStatus))
                || (BuyerStatus.SUSPENDED.equals(current.getBuyerStatus())
                        && BuyerStatus.ENABLED.equals(nextStatus));
        if (!validTransition) {
            throw new DomainValidationException("Invalid buyer commercial status transition");
        }
        current.setBuyerStatus(nextStatus);
        return buyerRepository.update(current);
    }

    private Buyer authoritative(Buyer targetBuyer) {
        DomainValidations.required(targetBuyer, "buyer");
        return buyerRepository.findByIdentifier(targetBuyer)
                .orElseThrow(() -> new DomainValidationException("Buyer not found"));
    }
}
