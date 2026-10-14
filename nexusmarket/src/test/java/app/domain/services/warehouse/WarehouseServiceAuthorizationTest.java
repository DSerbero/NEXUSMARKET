package app.domain.services.warehouse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.LogisticOperator;
import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.ports.out.SellerRepositoryPort;
import app.domain.ports.out.UserRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class WarehouseServiceAuthorizationTest {

    @Test
    void sellerCannotAssignLogisticsOperator() {
        WarehouseRepositoryPort warehouseRepository = mock(WarehouseRepositoryPort.class);
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        SellerRepositoryPort sellerRepository = mock(SellerRepositoryPort.class);
        WarehouseService service = new WarehouseService(warehouseRepository, userRepository, sellerRepository);
        Seller seller = new Seller();
        seller.setRole(UserRole.SELLER);
        seller.setStatus(UserStatus.ACTIVE);

        assertThrows(DomainValidationException.class,
                () -> service.assignLogisticsOperator(seller, new Warehouse(), new LogisticOperator()));
        verifyNoInteractions(warehouseRepository, userRepository, sellerRepository);
    }
}