package application.adapters.useCases.user;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import application.domain.exceptions.DomainValidationException;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;

class UserServiceAuthorizationTest {

    @Test
    void nonAdminCannotChangeAnotherUsersStatus() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        UserService service = new UserService(repository);
        User requester = user("buyer-1", UserRole.BUYER, UserStatus.ACTIVE);
        User target = user("seller-1", UserRole.SELLER, UserStatus.ACTIVE);

        assertThrows(DomainValidationException.class,
                () -> service.changeStatus(requester, target, UserStatus.BLOCKED));
        verifyNoInteractions(repository);
    }

    private User user(String identifier, UserRole role, UserStatus status) {
        User user = new User();
        user.setIdentifier(identifier);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}