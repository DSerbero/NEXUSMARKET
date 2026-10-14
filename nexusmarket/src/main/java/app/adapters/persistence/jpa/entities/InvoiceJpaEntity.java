package app.adapters.persistence.jpa.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Financial invoice issued for a paid order. One invoice per order
 * (enforced by {@code uk_invoices_order}).
 */
@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(
        name = "uk_invoices_order", columnNames = "order_identifier"))
@Getter
@Setter
@NoArgsConstructor
public class InvoiceJpaEntity {

    @Id
    @Column(name = "identifier", nullable = false, length = 36)
    private String identifier;

    @Column(name = "order_identifier", nullable = false, length = 36)
    private String orderIdentifier;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;
}
