package app.domain.models;

import java.math.BigDecimal;
import java.util.List;

import app.domain.exceptions.DomainValidationException;
import app.domain.valueobjects.ProductType;
import lombok.Getter;

@Getter
public class OrderItemSnapshot {

    public static final String CURRENCY_CODE = "COP";

    private final String productIdentifier;
    private final ProductType productType;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final List<String> variants;
    private final String sellerIdentifier;
    private final String digitalAssetReference;
    private final String currencyCode;

    public OrderItemSnapshot(Product product, int quantity) {
        if (product == null || product.getIdentifier() == null || product.getIdentifier().isBlank()) {
            throw new DomainValidationException("Product identifier is required for an order item");
        }
        if (product.getPrice() == null || product.getPrice().signum() <= 0) {
            throw new DomainValidationException("Product price must be greater than zero");
        }
        if (quantity <= 0) {
            throw new DomainValidationException("Quantity must be greater than zero");
        }
        if (product instanceof DigitalProduct digitalProduct) {
            productType = ProductType.DIGITAL;
            digitalAssetReference = digitalProduct.getDigitalAsset();
        } else if (product instanceof PhysicalProduct) {
            productType = ProductType.PHYSICAL;
            digitalAssetReference = null;
        } else {
            throw new DomainValidationException("Unsupported product type");
        }
        productIdentifier = product.getIdentifier();
        unitPrice = product.getPrice();
        this.quantity = quantity;
        variants = product.getVariants() == null ? List.of() : List.copyOf(product.getVariants());
        sellerIdentifier = product.getSeller() == null ? null : product.getSeller().getIdentifier();
        currencyCode = CURRENCY_CODE;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public boolean isDigital() {
        return ProductType.DIGITAL.equals(productType);
    }

    public boolean isPhysical() {
        return ProductType.PHYSICAL.equals(productType);
    }
}