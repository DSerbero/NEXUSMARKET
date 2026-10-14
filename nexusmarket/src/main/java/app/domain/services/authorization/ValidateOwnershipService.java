package app.domain.services.authorization;

import org.springframework.stereotype.Service;

import app.domain.exceptions.UnauthorizedOperationException;
import app.domain.services.DomainValidations;
import app.domain.models.User;

/**
 * Reusable authorization collaborator: ensures the acting {@link User} owns the
 * target resource (same identity), following the SDD "no primitive identifiers"
 * rule — ownership is decided on Domain Models.
 */
@Service
public class ValidateOwnershipService {

    public void execute(User user, User owner) {
        if (!DomainValidations.sameIdentity(user, owner)) {
            throw new UnauthorizedOperationException("User does not own the resource");
        }
    }
}
