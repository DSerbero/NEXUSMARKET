package app.domain.services.buyer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.User;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.services.DomainValidations;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Change Buyer Commercial Status: admin-only toggle between ENABLED and
 * SUSPENDED. The transition is a two-way switch; anything else is rejected.
 */
@Service
@ConditionalOnBean(BuyerRepositoryPort.class)
@RequiredArgsConstructor
public class ChangeBuyerCommercialStatusService {

    private final BuyerRepositoryPort buyerRepository;

    public Buyer execute(User requestingUser, Buyer buyer, BuyerStatus status) {
        DomainValidations.active(requestingUser);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("Only an admin can change buyer commercial status");
        }
        Buyer current = buyerRepository.findByIdentifier(buyer)
                .orElseThrow(() -> new DomainValidationException("Buyer not found"));
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
}