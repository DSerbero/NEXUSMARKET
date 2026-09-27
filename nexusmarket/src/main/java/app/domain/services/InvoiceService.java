package app.domain.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Invoice;
import app.domain.models.Order;
import app.domain.services.ports.InvoiceRepositoryPort;
import app.domain.services.ports.OrderRepositoryPort;
import app.domain.valueObjects.InvoiceStatus;
import app.domain.valueObjects.OrderStatus;
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
            throw new app.domain.services.exceptions.DomainValidationException("Only paid orders can be invoiced");
        }
        Invoice draft = new Invoice();
        draft.setOrder(current);
        if (invoiceRepository.findByOrder(draft).isPresent()) {
            throw new app.domain.services.exceptions.DomainValidationException("Order already has an invoice");
        }
        BigDecimal total = current.getItems().stream()
                .map(item -> item.getPrice() == null ? BigDecimal.ZERO : item.getPrice())
            .reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
        if (total.signum() <= 0) {
            throw new app.domain.services.exceptions.DomainValidationException("Invoice total must be greater than zero");
        }
        draft.setTotalAmount(total);
        draft.setIssueDate(LocalDateTime.now());
        draft.setInvoiceStatus(InvoiceStatus.ISSUED);
        return invoiceRepository.save(draft);
    }

    public Invoice voidInvoice(Invoice invoice) {
        invoice.setInvoiceStatus(InvoiceStatus.VOIDED);
        return invoiceRepository.update(invoice);
    }

    public Invoice markPaid(Invoice invoice) {
        invoice.setInvoiceStatus(InvoiceStatus.PAID);
        return invoiceRepository.update(invoice);
    }
}
