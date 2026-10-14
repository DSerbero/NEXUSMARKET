package app.domain.services.returns;

import app.domain.services.DomainValidations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Invoice;
import app.domain.models.Refund;
import app.domain.models.Return;
import app.domain.models.User;
import app.domain.exceptions.DomainValidationException;
import app.domain.ports.out.InvoiceRepositoryPort;
import app.domain.ports.out.RefundRepositoryPort;
import app.domain.ports.out.ReturnRepositoryPort;
import app.domain.valueobjects.RefundStatus;
import app.domain.valueobjects.ReturnStatus;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(RefundRepositoryPort.class)
@RequiredArgsConstructor
public class RefundService {

    private final ReturnRepositoryPort returnRepository;
    private final RefundRepositoryPort refundRepository;
    private final InvoiceRepositoryPort invoiceRepository;

    public Refund process(User requestingUser, Return returnRequest) {
        DomainValidations.active(requestingUser);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("Only an admin can process refunds");
        }
        Return current = authoritative(returnRequest);
        if (!ReturnStatus.COMPLETED.equals(current.getReturnStatus())) {
            throw new DomainValidationException("Invalid return status transition");
        }
        Refund refund = new Refund();
        refund.setRelatedReturn(current);
        if (refundRepository.findByReturn(refund).isPresent()) {
            throw new DomainValidationException("Return already has a refund");
        }
        Invoice invoiceQuery = new Invoice();
        invoiceQuery.setOrder(current.getOrder());
        Invoice invoice = invoiceRepository.findByOrder(invoiceQuery)
                .orElseThrow(() -> new DomainValidationException("Invoice not found for return order"));
        BigDecimal amount = DomainValidations.required(invoice.getTotalAmount(), "invoice.totalAmount");
        if (amount.signum() <= 0) {
            throw new DomainValidationException("Refund amount must be greater than zero");
        }
        refund.setRefundedAmount(amount);
            refund.setCurrencyCode(invoice.getCurrencyCode());
        refund.setRefundStatus(RefundStatus.PROCESSED);
        refund.setProcessingDate(LocalDateTime.now());
        return refundRepository.save(refund);
    }

    private Return authoritative(Return returnRequest) {
        DomainValidations.required(returnRequest, "returnRequest");
        return returnRepository.findByIdentifier(returnRequest)
                .orElseThrow(() -> new DomainValidationException("Return not found"));
    }
}