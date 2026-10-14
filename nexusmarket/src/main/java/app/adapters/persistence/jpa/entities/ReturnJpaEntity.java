package app.adapters.persistence.jpa.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Post-sale return requested by a buyer for a delivered order.
 */
@Entity
@Table(name = "returns")
@Getter
@Setter
@NoArgsConstructor
public class ReturnJpaEntity {

    @Id
    @Column(name = "identifier", nullable = false, length = 36)
    private String identifier;

    @Column(name = "order_identifier", nullable = false, length = 36)
    private String orderIdentifier;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;
}
