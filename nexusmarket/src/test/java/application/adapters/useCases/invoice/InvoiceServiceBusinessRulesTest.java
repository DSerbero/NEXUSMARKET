package application.adapters.useCases.invoice;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import application.domain.exceptions.DomainValidationException;
import application.domain.models.Invoice;
import application.domain.models.User;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueobjects.InvoiceStatus;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;

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