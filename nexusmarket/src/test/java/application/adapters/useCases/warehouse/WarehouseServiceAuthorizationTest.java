package application.adapters.useCases.warehouse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import application.domain.exceptions.DomainValidationException;
import application.domain.models.LogisticOperator;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;

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