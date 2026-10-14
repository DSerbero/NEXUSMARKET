package app.domain.services.authorization;

import org.springframework.stereotype.Service;

import app.domain.exceptions.UnauthorizedOperationException;
import app.domain.exceptions.ValidationException;
import app.domain.models.User;
import app.domain.valueobjects.UserStatus;

/**
 * Reusable authorization collaborator: ensures a {@link User} exists and is
 * operationally ACTIVE before a protected operation proceeds.
 */
@Service
public class ValidateActiveUserService {

    public void execute(User user) {
        if (user == null) {
            throw new ValidationException("user is required");
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new UnauthorizedOperationException("User must be active");
        }
    }
}
