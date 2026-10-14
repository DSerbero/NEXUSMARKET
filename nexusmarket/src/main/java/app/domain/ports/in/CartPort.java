package app.domain.ports.in;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.Order;
import app.domain.models.Product;

/**
 * Input port grouping the Cart Management use cases defined by the SDD
 * (Add Product to Cart, Remove Product from Cart, Consult Cart, Confirm Cart),
 * grouped by actor (buyer). The requesting identity arrives as a domain
 * {@link Buyer} reference; the use-case adapter resolves it authoritatively.
 */
public interface CartPort {

    Cart consult(Buyer requestingBuyer);

    Cart addProduct(Buyer requestingBuyer, Product product);

    Cart removeProduct(Buyer requestingBuyer, Product product);

    Order confirm(Buyer requestingBuyer);
}