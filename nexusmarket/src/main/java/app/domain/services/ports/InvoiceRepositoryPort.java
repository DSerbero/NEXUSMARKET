package app.domain.services.ports;

import java.util.Optional;

import app.domain.models.Invoice;

public interface InvoiceRepositoryPort {
    Invoice save(Invoice invoice);
    Optional<Invoice> findByOrder(Invoice invoice);
    Optional<Invoice> findByIdentifier(Invoice invoice);
    Invoice update(Invoice invoice);
}
