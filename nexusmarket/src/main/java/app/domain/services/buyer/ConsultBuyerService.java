package app.domain.services.buyer;

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
 * Consult Buyer: the buyer themselves, or an ADMIN/SUPERVISOR, may read the
 * buyer profile.
 */
@Service
@ConditionalOnBean(BuyerRepositoryPort.class)
@RequiredArgsConstructor
public class ConsultBuyerService {

    private final BuyerRepositoryPort buyerRepository;

    public Buyer execute(User requestingUser, Buyer targetBuyer) {
        DomainValidations.active(requestingUser);
        Buyer current = buyerRepository.findByIdentifier(targetBuyer)
                .orElseThrow(() -> new DomainValidationException("Buyer not found"));
        if (!DomainValidations.sameIdentity(requestingUser, current)
                && !UserRole.ADMIN.equals(requestingUser.getRole())
                && !UserRole.SUPERVISOR.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot consult this buyer");
        }
        return current;
    }
}