package app.adapters.persistence.jpa.entities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_order_item_position", columnNames = {"order_identifier", "position"}))
@Getter
@Setter
@NoArgsConstructor
public class OrderItemJpaEntity {

    @Id
    @Column(name = "identifier", nullable = false, length = 36)
    private String identifier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_identifier", nullable = false)
    private OrderJpaEntity order;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "product_identifier", nullable = false, length = 36)
    private String productIdentifier;

    @Column(name = "product_type", nullable = false, length = 20)
    private String productType;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "seller_identifier", length = 36)
    private String sellerIdentifier;

    @Column(name = "digital_asset_reference", length = 1000)
    private String digitalAssetReference;

    @ElementCollection
    @CollectionTable(name = "order_item_variants",
            joinColumns = @JoinColumn(name = "order_item_identifier"))
    @OrderColumn(name = "position")
    @Column(name = "variant_value", nullable = false, length = 200)
    private List<String> variants = new ArrayList<>();
}