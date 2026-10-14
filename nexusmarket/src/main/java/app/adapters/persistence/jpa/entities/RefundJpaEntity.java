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
 * Financial refund issued for an approved and completed return. One refund per
 * return (enforced by {@code uk_refunds_return}).
 */
@Entity
@Table(name = "refunds", uniqueConstraints = @UniqueConstraint(
        name = "uk_refunds_return", columnNames = "return_identifier"))
@Getter
@Setter
@NoArgsConstructor
public class RefundJpaEntity {

    @Id
    @Column(name = "identifier", nullable = false, length = 36)
    private String identifier;

    @Column(name = "return_identifier", nullable = false, length = 36)
    private String returnIdentifier;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "processing_date", nullable = false)
    private LocalDateTime processingDate;
}
