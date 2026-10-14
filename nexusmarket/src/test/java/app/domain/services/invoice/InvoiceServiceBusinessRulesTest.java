package app.domain.services.invoice;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Invoice;
import app.domain.models.User;
import app.domain.ports.out.InvoiceRepositoryPort;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.valueobjects.InvoiceStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class InvoiceServiceBusinessRulesTest {

    @Test
    void nonAdminCannotVoidInvoice() {
        InvoiceRepositoryPort invoiceRepository = mock(InvoiceRepositoryPort.class);
        InvoiceService service = new InvoiceService(invoiceRepository, mock(OrderRepositoryPort.class));
        User buyer = new User();
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);

        assertThrows(DomainValidationException.class, () -> service.voidInvoice(buyer, new Invoice()));
        verifyNoInteractions(invoiceRepository);
    }

    @Test
    void voidInvoiceCannotBeVoidedTwice() {
        InvoiceRepositoryPort invoiceRepository = mock(InvoiceRepositoryPort.class);
        InvoiceService service = new InvoiceService(invoiceRepository, mock(OrderRepositoryPort.class));
        Invoice invoice = new Invoice();
        invoice.setInvoiceStatus(InvoiceStatus.VOIDED);
        org.mockito.Mockito.when(invoiceRepository.findByIdentifier(invoice))
                .thenReturn(java.util.Optional.of(invoice));
        User admin = new User();
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        assertThrows(DomainValidationException.class, () -> service.voidInvoice(admin, invoice));
    }
}