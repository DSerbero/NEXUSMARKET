package application.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import application.domain.valueobjects.RefundStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Refund {
    private Return relatedReturn;
    private BigDecimal refundedAmount;
    private RefundStatus refundStatus;
    private LocalDateTime processingDate;
}
