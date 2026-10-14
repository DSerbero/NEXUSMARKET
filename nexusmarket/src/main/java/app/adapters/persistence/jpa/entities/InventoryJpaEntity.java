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
 * Physical stock for a product inside a warehouse. The product lives in MongoDB,
 * so only a logical {@code product_identifier} reference is stored (no FK), as
 * decided in the Persistence and Consistency doc.
 */
@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(
        name = "uk_inventory_product_warehouse", columnNames = {"product_identifier", "warehouse_identifier"}))
@Getter
@Setter
@NoArgsConstructor
public class InventoryJpaEntity {

    @Id
    @Column(name = "identifier", nullable = false, length = 36)
    private String identifier;

    @Column(name = "product_identifier", nullable = false, length = 36)
    private String productIdentifier;

    @Column(name = "warehouse_identifier", nullable = false)
    private Long warehouseIdentifier;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "movement_type", nullable = false, length = 20)
    private String movementType;

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;
}
