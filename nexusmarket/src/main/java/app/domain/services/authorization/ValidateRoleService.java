package app.domain.services.authorization;

import org.springframework.stereotype.Service;

import app.domain.exceptions.UnauthorizedOperationException;
import app.domain.exceptions.ValidationException;
import app.domain.models.User;
import app.domain.valueobjects.UserRole;

/**
 * Reusable authorization collaborator: ensures a {@link User} holds one of the
 * required roles for the operation.
 */
@Service
public class ValidateRoleService {

    public void execute(User user, UserRole required) {
        if (user == null) {
            throw new ValidationException("user is required");
        }
        if (!required.equals(user.getRole())) {
            throw new UnauthorizedOperationException("Role " + required.getCode() + " is required");
        }
    }

    public void execute(User user, UserRole... allowed) {
        if (user == null) {
            throw new ValidationException("user is required");
        }
        for (UserRole role : allowed) {
            if (role.equals(user.getRole())) {
                return;
            }
        }
        throw new UnauthorizedOperationException("User role is not allowed for this operation");
    }
}
