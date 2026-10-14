package app.adapters.persistence.jpa.entities;

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
 * Logistics shipment for an order containing physical products. One shipment
 * per order (enforced by {@code uk_shipments_order}).
 */
@Entity
@Table(name = "shipments", uniqueConstraints = @UniqueConstraint(
        name = "uk_shipments_order", columnNames = "order_identifier"))
@Getter
@Setter
@NoArgsConstructor
public class ShipmentJpaEntity {

    @Id
    @Column(name = "identifier", nullable = false, length = 36)
    private String identifier;

    @Column(name = "order_identifier", nullable = false, length = 36)
    private String orderIdentifier;

    @Column(name = "origin_warehouse_identifier", nullable = false)
    private Long originWarehouseIdentifier;

    @Column(name = "operator_identifier", nullable = false, length = 36)
    private String operatorIdentifier;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "dispatch_date")
    private LocalDateTime dispatchDate;
}
