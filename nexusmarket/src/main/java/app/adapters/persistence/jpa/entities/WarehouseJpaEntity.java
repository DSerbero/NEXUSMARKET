package app.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Warehouse owned either by the Marketplace or by a Seller. The owner and the
 * responsible user are stored as plain references to {@code users} because the
 * owner is polymorphic (Seller or Admin) and only the identifier is needed for
 * authorization decisions.
 */
@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
public class WarehouseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identifier", nullable = false)
    private Long identifier;

    @Column(name = "location", nullable = false, length = 500)
    private String location;

    @Column(name = "owner_type", nullable = false, length = 20)
    private String ownerType;

    @Column(name = "owner_identifier", nullable = false, length = 36)
    private String ownerIdentifier;

    @Column(name = "responsible_user_identifier", length = 36)
    private String responsibleUserIdentifier;
}
