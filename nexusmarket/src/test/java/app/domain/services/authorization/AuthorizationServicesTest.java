package app.domain.services.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.UnauthorizedOperationException;
import app.domain.exceptions.ValidationException;
import app.domain.models.User;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class AuthorizationServicesTest {

    private final ValidateActiveUserService validateActiveUser = new ValidateActiveUserService();
    private final ValidateRoleService validateRole = new ValidateRoleService();
    private final ValidateOwnershipService validateOwnership = new ValidateOwnershipService();

    @Test
    void activeUserPassesAndInactiveIsRejected() {
        User active = user("u1", UserStatus.ACTIVE);
        assertDoesNotThrow(() -> validateActiveUser.execute(active));

        User blocked = user("u1", UserStatus.BLOCKED);
        assertThrows(UnauthorizedOperationException.class, () -> validateActiveUser.execute(blocked));
        assertThrows(ValidationException.class, () -> validateActiveUser.execute(null));
    }

    @Test
    void roleValidationAcceptsRequiredAndRejectsOthers() {
        User seller = user("u1", UserStatus.ACTIVE);
        seller.setRole(UserRole.SELLER);

        assertDoesNotThrow(() -> validateRole.execute(seller, UserRole.SELLER));
        assertThrows(UnauthorizedOperationException.class, () -> validateRole.execute(seller, UserRole.ADMIN));
    }

    @Test
    void multiRoleValidationAcceptsAnyAllowedRole() {
        User admin = user("u1", UserStatus.ACTIVE);
        admin.setRole(UserRole.ADMIN);

        assertDoesNotThrow(() -> validateRole.execute(admin, UserRole.ADMIN, UserRole.SUPERVISOR));
        assertThrows(UnauthorizedOperationException.class,
                () -> validateRole.execute(admin, UserRole.SELLER, UserRole.BUYER));
    }

    @Test
    void ownershipValidationComparesIdentity() {
        User owner = user("owner", UserStatus.ACTIVE);
        User sameRef = user("owner", UserStatus.ACTIVE);
        User other = user("other", UserStatus.ACTIVE);

        assertDoesNotThrow(() -> validateOwnership.execute(owner, sameRef));
        assertThrows(UnauthorizedOperationException.class, () -> validateOwnership.execute(owner, other));
    }

    private User user(String identifier, UserStatus status) {
        User user = new User();
        user.setIdentifier(identifier);
        user.setStatus(status);
        return user;
    }
}
