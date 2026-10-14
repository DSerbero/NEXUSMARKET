package app.adapters.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import app.adapters.rest.controllers.CartController;
import app.adapters.rest.mappers.CartRestMapper;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.CartItem;
import app.domain.models.DigitalProduct;
import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;
import app.domain.models.PhysicalProduct;
import app.domain.models.Product;
import app.domain.ports.in.CartPort;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.ProductStatus;

@WebMvcTest(CartController.class)
@Import(CartRestMapper.class)
class CartControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartPort cartPort;

    @Test
    @WithMockUser(username = "buyer-1")
    void buyerCanConsultOwnCart() throws Exception {
        when(cartPort.consult(any(Buyer.class))).thenReturn(cartWithOneItem());

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productIdentifier").value("product-1"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(10.00));
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void buyerIdentityComesFromSecurityContext() throws Exception {
        when(cartPort.consult(any(Buyer.class))).thenReturn(cartWithOneItem());

        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isOk());

        ArgumentCaptor<Buyer> buyerCaptor = ArgumentCaptor.forClass(Buyer.class);
        verify(cartPort).consult(buyerCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("buyer-1", buyerCaptor.getValue().getIdentifier());
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void buyerCanAddItemToCart() throws Exception {
        when(cartPort.addProduct(any(Buyer.class), any(Product.class))).thenReturn(cartWithOneItem());

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"product-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productIdentifier").value("product-1"));
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void blankProductIdIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("productId is required"));
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void buyerCanRemoveItemFromCart() throws Exception {
        when(cartPort.removeProduct(any(Buyer.class), any(Product.class))).thenReturn(new Cart());

        mockMvc.perform(delete("/api/v1/cart/items/{productId}", "product-1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void productMissingFromCartIsRejected() throws Exception {
        when(cartPort.removeProduct(any(Buyer.class), any(Product.class)))
                .thenThrow(new DomainValidationException("Product is not in cart"));

        mockMvc.perform(delete("/api/v1/cart/items/{productId}", "product-x").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product is not in cart"));
    }

    @Test
    @WithMockUser(username = "buyer-1")
    void checkoutReturnsCreatedOrder() throws Exception {
        when(cartPort.confirm(any(Buyer.class))).thenReturn(confirmedOrder());

        mockMvc.perform(post("/api/v1/cart/checkout").with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identifier").value("order-1"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items[0].productIdentifier").value("product-1"))
                .andExpect(jsonPath("$.items[0].lineTotal").value(30.00))
                .andExpect(jsonPath("$.items[0].currencyCode").value("COP"));
    }

    @Test
    void unauthenticatedUserCannotUseCart() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    private Cart cartWithOneItem() {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setPrice(new BigDecimal("10.00"));
        product.setStatus(ProductStatus.PUBLISHED);
        product.setDigitalAsset("asset-1");

        Cart cart = new Cart();
        cart.setCreationDate(LocalDateTime.of(2026, 10, 14, 10, 0));
        cart.setItems(new ArrayList<>(List.of(new CartItem(product, 2))));
        return cart;
    }

    private Order confirmedOrder() {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setPrice(new BigDecimal("10.00"));
        product.setStatus(ProductStatus.PUBLISHED);
        product.setDigitalAsset("asset-1");

        Order order = new Order();
        order.setIdentifier("order-1");
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreationDate(LocalDateTime.of(2026, 10, 14, 10, 5));
        order.setItems(new ArrayList<>(List.of(new OrderItemSnapshot(product, 3))));
        return order;
    }
}