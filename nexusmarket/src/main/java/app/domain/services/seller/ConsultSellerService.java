package app.domain.services.seller;

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
 * Consult Seller: the seller themselves, or an ADMIN/SUPERVISOR, may read the
 * seller profile.
 */
@Service
@ConditionalOnBean(SellerRepositoryPort.class)
@RequiredArgsConstructor
public class ConsultSellerService {

    private final SellerRepositoryPort sellerRepository;

    public Seller execute(User requestingUser, Seller seller) {
        DomainValidations.active(requestingUser);
        Seller current = sellerRepository.findByIdentifier(seller)
                .orElseThrow(() -> new DomainValidationException("Seller not found"));
        if (!DomainValidations.sameIdentity(requestingUser, current)
                && !UserRole.ADMIN.equals(requestingUser.getRole())
                && !UserRole.SUPERVISOR.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot consult this seller");
        }
        return current;
    }
}