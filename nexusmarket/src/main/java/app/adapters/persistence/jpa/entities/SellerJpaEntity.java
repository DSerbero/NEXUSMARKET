package app.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Seller identity profile, 1:1 with {@link UserJpaEntity} and sharing the same
 * identifier, mirroring the Buyer persistence decision (see Persistence doc).
 */
@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor
public class SellerJpaEntity {

    @Id
    @Column(name = "user_identifier", nullable = false, length = 36)
    private String userIdentifier;

    @MapsId
    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @jakarta.persistence.JoinColumn(name = "user_identifier", nullable = false)
    private UserJpaEntity user;
}
