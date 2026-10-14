package application.adapters.useCases.invoice;

import application.domain.services.DomainValidations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.exceptions.DomainValidationException;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueobjects.InvoiceStatus;
import application.domain.valueobjects.OrderStatus;
import application.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(InvoiceRepositoryPort.class)
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepositoryPort invoiceRepository;
    private final OrderRepositoryPort orderRepository;

    public Invoice issue(Order order) {
        Order current = orderRepository.findByIdentifier(order).orElseThrow();
        if (!OrderStatus.PAID.equals(current.getOrderStatus())) {
            throw new application.domain.exceptions.DomainValidationException("Only paid orders can be invoiced");
        }
        Invoice draft = new Invoice();
        draft.setOrder(current);
        if (invoiceRepository.findByOrder(draft).isPresent()) {
            throw new application.domain.exceptions.DomainValidationException("Order already has an invoice");
        }
        BigDecimal total = current.getItems().stream()
                .map(item -> {
                    if (item.getPrice() == null) {
                        throw new DomainValidationException("Product price is required for invoicing");
                    }
                    return item.getPrice();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            throw new DomainValidationException("Invoice total must be greater than zero");
        }
        draft.setTotalAmount(total);
        draft.setIssueDate(LocalDateTime.now());
        draft.setInvoiceStatus(InvoiceStatus.ISSUED);
        return invoiceRepository.save(draft);
    }

    public Invoice voidInvoice(User requestingUser, Invoice invoice) {
        DomainValidations.active(requestingUser);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("Only an admin can void an invoice");
        }
        Invoice current = authoritative(invoice);
        if (InvoiceStatus.VOIDED.equals(current.getInvoiceStatus())) {
            throw new DomainValidationException("Invoice is already voided");
        }
        current.setInvoiceStatus(InvoiceStatus.VOIDED);
        return invoiceRepository.update(current);
    }

    public Invoice markPaid(Invoice invoice) {
        Invoice current = authoritative(invoice);
        if (!InvoiceStatus.ISSUED.equals(current.getInvoiceStatus())) {
            throw new DomainValidationException("Only issued invoices can be marked paid");
        }
        current.setInvoiceStatus(InvoiceStatus.PAID);
        return invoiceRepository.update(current);
    }

    private Invoice authoritative(Invoice invoice) {
        DomainValidations.required(invoice, "invoice");
        return invoiceRepository.findByIdentifier(invoice)
                .orElseThrow(() -> new DomainValidationException("Invoice not found"));
    }
}
