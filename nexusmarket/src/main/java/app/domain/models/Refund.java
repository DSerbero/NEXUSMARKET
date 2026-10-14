package app.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import app.domain.valueobjects.RefundStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Refund {
    private String identifier;
    private Return relatedReturn;
    private BigDecimal refundedAmount;
    private String currencyCode;
    private RefundStatus refundStatus;
    private LocalDateTime processingDate;
}
