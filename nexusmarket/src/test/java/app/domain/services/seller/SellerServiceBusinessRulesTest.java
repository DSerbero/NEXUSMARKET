package app.domain.services.seller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.ports.out.SellerRepositoryPort;
import app.domain.ports.out.UserRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import app.domain.valueobjects.WarehouseOwnerType;

class SellerServiceBusinessRulesTest {

    @Test
    void adminRegistrationCreatesActiveSellerAndOwnedInitialWarehouse() {
        SellerRepositoryPort sellerRepository = mock(SellerRepositoryPort.class);
        UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
        WarehouseRepositoryPort warehouseRepository = mock(WarehouseRepositoryPort.class);
        SellerService service = new SellerService(sellerRepository, userRepository, warehouseRepository);
        User admin = new User();
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        Seller seller = new Seller();
        seller.setFullName("Seller");
        seller.setEmail("seller@example.test");
        Warehouse warehouse = new Warehouse();
        warehouse.setLocation("Initial warehouse");
        when(sellerRepository.save(seller)).thenReturn(seller);

        Seller registered = service.register(admin, seller, warehouse);

        assertEquals(UserRole.SELLER, registered.getRole());
        assertEquals(UserStatus.ACTIVE, registered.getStatus());
        assertEquals(WarehouseOwnerType.SELLER, warehouse.getOwnerType());
        assertEquals(registered, warehouse.getOwner());
        assertEquals(registered, warehouse.getResponsibleUser());
    }
}