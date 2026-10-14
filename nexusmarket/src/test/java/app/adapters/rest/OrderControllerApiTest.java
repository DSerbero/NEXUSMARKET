package app.adapters.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import app.adapters.rest.controllers.OrderController;
import app.adapters.rest.mappers.OrderRestMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.exceptions.EntityNotFoundException;
import app.domain.exceptions.UnauthorizedOperationException;
import app.domain.models.Buyer;
import app.domain.models.DigitalProduct;
import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;
import app.domain.models.User;
import app.domain.ports.in.OrderPort;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.ProductStatus;

@WebMvcTest(OrderController.class)
@Import(OrderRestMapper.class)
class OrderControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderPort orderPort;

    @Test
    @WithMockUser(username = "buyer-1")
    void buyerCanConsultOwnOrder() throws Exception {
        when(orderPort.consult(any(User.class), any(Order.class)))
                .thenReturn(order(OrderStatus.PENDING_PAYMENT));

        mockMvc.perform(get("/api/v1/orders/{id}", "order-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("order-1"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items[0].productIdentifier").value("product-1"))
                .andExpect(jsonPath("$.items[0].currencyCode").value("COP"));
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void confirmPaymentReturnsPaidOrder() throws Exception {
        when(orderPort.confirmPayment(any(User.class), any(Order.class)))
                .thenReturn(order(OrderStatus.PAID));

        mockMvc.perform(post("/api/v1/orders/{id}/payment", "order-1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @WithMockUser(username = "logistics-1")
    void logisticsOperatorCanDispatchOrder() throws Exception {
        when(orderPort.dispatch(any(User.class), any(Order.class)))
                .thenReturn(order(OrderStatus.DISPATCHED));

        mockMvc.perform(post("/api/v1/orders/{id}/dispatch", "order-1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPATCHED"));
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void finalizeReturnsDeliveredOrder() throws Exception {
        when(orderPort.finalizeOrder(any(User.class), any(Order.class)))
                .thenReturn(order(OrderStatus.DELIVERED_FINALIZED));

        mockMvc.perform(post("/api/v1/orders/{id}/finalize", "order-1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED_FINALIZED"));
    }

    @Test
    @WithMockUser(username = "buyer-2")
    void buyerCannotConsultAnotherBuyersOrder() throws Exception {
        when(orderPort.consult(any(User.class), any(Order.class)))
                .thenThrow(new UnauthorizedOperationException("User does not own the resource"));

        mockMvc.perform(get("/api/v1/orders/{id}", "order-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void unknownOrderIsReturnedAsNotFound() throws Exception {
        when(orderPort.consult(any(User.class), any(Order.class)))
                .thenThrow(new EntityNotFoundException("Order"));

        mockMvc.perform(get("/api/v1/orders/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void invalidStatusTransitionIsRejected() throws Exception {
        when(orderPort.confirmPayment(any(User.class), any(Order.class)))
                .thenThrow(new DomainValidationException("Invalid order status transition"));

        mockMvc.perform(post("/api/v1/orders/{id}/payment", "order-1").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid order status transition"));
    }

    @Test
    void unauthenticatedUserCannotConsultOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", "order-1"))
                .andExpect(status().isUnauthorized());
    }

    private Order order(OrderStatus status) {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setPrice(new BigDecimal("10.00"));
        product.setStatus(ProductStatus.PUBLISHED);
        product.setDigitalAsset("asset-1");

        Buyer buyer = new Buyer();
        buyer.setIdentifier("buyer-1");

        Order order = new Order();
        order.setIdentifier("order-1");
        order.setBuyer(buyer);
        order.setOrderStatus(status);
        order.setCreationDate(LocalDateTime.of(2026, 10, 14, 12, 0));
        order.setItems(new ArrayList<>(List.of(new OrderItemSnapshot(product, 3))));
        return order;
    }
}