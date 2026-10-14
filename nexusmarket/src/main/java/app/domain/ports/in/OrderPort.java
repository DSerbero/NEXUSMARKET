package app.domain.ports.in;

import app.domain.models.Order;
import app.domain.models.User;

/**
 * Input port grouping the Order Management use cases defined by the SDD
 * (Consult Order, Confirm Order Payment, Dispatch Order, Finalize Order).
 * Create Order is intentionally absent: orders are only created through
 * {@link CartPort#confirm} (Confirm Cart), never directly.
 *
 * <p>Authorization differs per operation: the owning buyer consults, pays and
 * finalizes; the assigned logistics operator dispatches. The requesting
 * identity arrives as a domain {@link User} reference resolved authoritatively
 * by the use-case adapter.</p>
 */
public interface OrderPort {

    Order consult(User requestingUser, Order order);

    Order confirmPayment(User requestingUser, Order order);

    Order dispatch(User requestingUser, Order order);

    Order finalizeOrder(User requestingUser, Order order);
}