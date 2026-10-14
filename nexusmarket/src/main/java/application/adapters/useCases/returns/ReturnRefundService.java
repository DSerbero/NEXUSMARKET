package application.adapters.useCases.returns;

import application.domain.services.DomainValidations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Buyer;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.exceptions.DomainValidationException;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
import application.domain.valueobjects.RefundStatus;
import application.domain.valueobjects.ReturnStatus;
import application.domain.valueobjects.OrderStatus;
import application.domain.valueobjects.BuyerStatus;
import application.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(ReturnRepositoryPort.class)
@RequiredArgsConstructor
public class ReturnRefundService {

    private final ReturnRepositoryPort returnRepository;
    private final RefundRepositoryPort refundRepository;
    private final OrderRepositoryPort orderRepository;
    private final InvoiceRepositoryPort invoiceRepository;

    public Return request(Buyer requestingBuyer, Order order, String reason) {
        DomainValidations.active(requestingBuyer);
        if (!BuyerStatus.ENABLED.equals(requestingBuyer.getBuyerStatus())) {
            throw new DomainValidationException("Buyer is not enabled");
        }
        DomainValidations.required(order, "order");
        Order current = orderRepository.findByIdentifier(order)
                .orElseThrow(() -> new DomainValidationException("Order not found"));
        if (!DomainValidations.sameIdentity(requestingBuyer, current.getBuyer())) {
            throw new DomainValidationException("Buyer cannot request a return for another order");
        }
        if (!OrderStatus.DELIVERED_FINALIZED.equals(current.getOrderStatus())) {
            throw new DomainValidationException("Order is not eligible for return");
        }
        DomainValidations.requiredText(reason, "reason");
        Return request = new Return();
        request.setOrder(current);
        if (returnRepository.findActiveByOrder(request).isPresent()) {
            throw new DomainValidationException("Order already has an active return");
        }
        request.setReason(reason);
        request.setReturnStatus(ReturnStatus.REQUESTED);
        request.setRequestDate(LocalDateTime.now());
        return returnRepository.save(request);
    }

    public Return approve(User requestingUser, Return returnRequest) {
        requireDecisionMaker(requestingUser);
        Return current = authoritative(returnRequest);
        requireStatus(current, ReturnStatus.REQUESTED);
        current.setReturnStatus(ReturnStatus.APPROVED);
        return returnRepository.update(current);
    }

    public Return reject(User requestingUser, Return returnRequest) {
        requireDecisionMaker(requestingUser);
        Return current = authoritative(returnRequest);
        requireStatus(current, ReturnStatus.REQUESTED);
        current.setReturnStatus(ReturnStatus.REJECTED);
        return returnRepository.update(current);
    }

    public Return complete(User requestingUser, Return returnRequest) {
        requireDecisionMaker(requestingUser);
        Return current = authoritative(returnRequest);
        requireStatus(current, ReturnStatus.APPROVED);
        current.setReturnStatus(ReturnStatus.COMPLETED);
        return returnRepository.update(current);
    }

    public Refund processRefund(User requestingUser, Return returnRequest) {
        DomainValidations.active(requestingUser);
        if (!UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("Only an admin can process refunds");
        }
        Return current = authoritative(returnRequest);
        requireStatus(current, ReturnStatus.COMPLETED);
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
        refund.setRefundStatus(RefundStatus.PROCESSED);
        refund.setProcessingDate(LocalDateTime.now());
        return refundRepository.save(refund);
    }

    private void requireStatus(Return returnRequest, ReturnStatus expected) {
        DomainValidations.required(returnRequest, "returnRequest");
        if (!expected.equals(returnRequest.getReturnStatus())) {
            throw new DomainValidationException("Invalid return status transition");
        }
    }

    private Return authoritative(Return returnRequest) {
        DomainValidations.required(returnRequest, "returnRequest");
        return returnRepository.findByIdentifier(returnRequest)
                .orElseThrow(() -> new DomainValidationException("Return not found"));
    }

    private void requireDecisionMaker(User user) {
        DomainValidations.active(user);
        if (!UserRole.ADMIN.equals(user.getRole())
                && !UserRole.LOGISTICS_OPERATOR.equals(user.getRole())) {
            throw new DomainValidationException("Only Admin or LogisticsOperator can manage returns");
        }
    }
}
