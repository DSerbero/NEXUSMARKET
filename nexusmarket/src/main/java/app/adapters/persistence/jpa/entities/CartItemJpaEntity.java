package app.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cart_item_position", columnNames = {"cart_identifier", "position"}),
        @UniqueConstraint(name = "uk_cart_product", columnNames = {"cart_identifier", "product_identifier"})
})
@Getter
@Setter
@NoArgsConstructor
public class CartItemJpaEntity {

    @Id
    @Column(name = "identifier", nullable = false, length = 36)
    private String identifier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_identifier", nullable = false)
    private CartJpaEntity cart;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "product_identifier", nullable = false, length = 36)
    private String productIdentifier;

    @Column(name = "product_type", nullable = false, length = 20)
    private String productType;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}