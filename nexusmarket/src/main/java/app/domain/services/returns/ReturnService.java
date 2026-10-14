package app.domain.services.returns;

import app.domain.services.DomainValidations;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Buyer;
import app.domain.models.Order;
import app.domain.models.Return;
import app.domain.models.User;
import app.domain.exceptions.DomainValidationException;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.ReturnRepositoryPort;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.ReturnStatus;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(ReturnRepositoryPort.class)
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRepositoryPort returnRepository;
    private final OrderRepositoryPort orderRepository;

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