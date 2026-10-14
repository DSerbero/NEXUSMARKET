package app.domain.models;

import app.domain.exceptions.DomainValidationException;
import lombok.Getter;

@Getter
public class CartItem {

    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        if (product == null) {
            throw new DomainValidationException("Product must be provided");
        }
        if (quantity <= 0) {
            throw new DomainValidationException("Quantity must be greater than zero");
        }
        this.product = product;
        this.quantity = quantity;
    }

    public void increment() {
        if (quantity == Integer.MAX_VALUE) {
            throw new DomainValidationException("Quantity exceeds the supported range");
        }
        quantity++;
    }

    public boolean decrement() {
        if (quantity <= 0) {
            throw new DomainValidationException("Quantity must be greater than zero");
        }
        quantity--;
        return quantity == 0;
    }
}