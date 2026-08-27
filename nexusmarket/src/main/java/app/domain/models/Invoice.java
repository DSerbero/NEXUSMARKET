package app.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import app.domain.valueObjects.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Invoice {
    private Order order;
    private BigDecimal totalAmount;
    private LocalDateTime issueDate;
    private InvoiceStatus invoiceStatus;
}
