package app.domain.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Invoice;
import app.domain.models.Order;
import app.domain.models.Refund;
import app.domain.models.Return;
import app.domain.models.User;
import app.domain.services.ports.InvoiceRepositoryPort;
import app.domain.services.ports.OrderRepositoryPort;
import app.domain.services.ports.RefundRepositoryPort;
import app.domain.services.ports.ReturnRepositoryPort;
import app.domain.valueObjects.RefundStatus;
import app.domain.valueObjects.ReturnStatus;
import app.domain.valueObjects.OrderStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(ReturnRepositoryPort.class)
@RequiredArgsConstructor
public class ReturnRefundService {

    private final ReturnRepositoryPort returnRepository;
    private final RefundRepositoryPort refundRepository;
    private final OrderRepositoryPort orderRepository;
    private final InvoiceRepositoryPort invoiceRepository;

    public Return request(User requestingUser, Order order, String reason) {
        DomainValidations.active(requestingUser);
        Order current = orderRepository.findByIdentifier(order).orElseThrow();
        if (!OrderStatus.DELIVERED_FINALIZED.equals(current.getOrderStatus())) {
            throw new app.domain.services.exceptions.DomainValidationException("Order is not eligible for return");
        }
        DomainValidations.requiredText(reason, "reason");
        Return request = new Return();
        request.setOrder(current);
        request.setReason(reason);
        request.setReturnStatus(ReturnStatus.REQUESTED);
        request.setRequestDate(LocalDateTime.now());
        return returnRepository.save(request);
    }

    public Return approve(Return returnRequest) {
        requireStatus(returnRequest, ReturnStatus.REQUESTED);
        returnRequest.setReturnStatus(ReturnStatus.APPROVED);
        return returnRepository.update(returnRequest);
    }

    public Return reject(Return returnRequest) {
        requireStatus(returnRequest, ReturnStatus.REQUESTED);
        returnRequest.setReturnStatus(ReturnStatus.REJECTED);
        return returnRepository.update(returnRequest);
    }

    public Return complete(Return returnRequest) {
        requireStatus(returnRequest, ReturnStatus.APPROVED);
        returnRequest.setReturnStatus(ReturnStatus.COMPLETED);
        return returnRepository.update(returnRequest);
    }

    public Refund processRefund(Return returnRequest) {
        requireStatus(returnRequest, ReturnStatus.COMPLETED);
        Refund refund = new Refund();
        refund.setRelatedReturn(returnRequest);
        if (refundRepository.findByReturn(refund).isPresent()) {
            throw new app.domain.services.exceptions.DomainValidationException("Return already has a refund");
        }
        Invoice invoiceQuery = new Invoice();
        invoiceQuery.setOrder(returnRequest.getOrder());
        Invoice invoice = invoiceRepository.findByOrder(invoiceQuery).orElseThrow();
        refund.setRefundedAmount(invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount());
        refund.setRefundStatus(RefundStatus.PROCESSED);
        refund.setProcessingDate(LocalDateTime.now());
        return refundRepository.save(refund);
    }

    private void requireStatus(Return returnRequest, ReturnStatus expected) {
        DomainValidations.required(returnRequest, "returnRequest");
        if (!expected.equals(returnRequest.getReturnStatus())) {
            throw new app.domain.services.exceptions.DomainValidationException("Invalid return status transition");
        }
    }
}
