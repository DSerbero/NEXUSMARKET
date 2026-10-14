package app.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import app.domain.valueobjects.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Invoice {
    private Order order;
    private BigDecimal totalAmount;
    private String currencyCode;
    private LocalDateTime issueDate;
    private InvoiceStatus invoiceStatus;
}
