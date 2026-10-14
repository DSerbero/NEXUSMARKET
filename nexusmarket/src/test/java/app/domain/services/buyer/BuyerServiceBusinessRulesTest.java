package app.domain.services.buyer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.User;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class BuyerServiceBusinessRulesTest {

    @Test
    void registrationSetsBuyerRoleAndActiveStatuses() {
        BuyerRepositoryPort repository = mock(BuyerRepositoryPort.class);
        BuyerService service = new BuyerService(repository);
        Buyer buyer = new Buyer();
        buyer.setFullName("Buyer");
        buyer.setEmail("buyer@example.test");
        buyer.setPrimaryAddress("Address");
        when(repository.save(buyer)).thenReturn(buyer);

        Buyer registered = service.register(buyer);

        assertEquals(UserRole.BUYER, registered.getRole());
        assertEquals(UserStatus.ACTIVE, registered.getStatus());
        assertEquals(BuyerStatus.ENABLED, registered.getBuyerStatus());
    }

    @Test
    void buyerCannotChangeCommercialStatus() {
        BuyerRepositoryPort repository = mock(BuyerRepositoryPort.class);
        BuyerService service = new BuyerService(repository);
        User buyer = new User();
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);

        assertThrows(DomainValidationException.class,
                () -> service.changeCommercialStatus(buyer, new Buyer(), BuyerStatus.SUSPENDED));
        verifyNoInteractions(repository);
    }
}