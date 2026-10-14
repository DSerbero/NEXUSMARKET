package app.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import app.adapters.persistence.jpa.entities.UserCredentialJpaEntity;
import app.adapters.persistence.jpa.repositories.UserCredentialJpaRepository;
import app.domain.models.Buyer;
import app.domain.models.DigitalProduct;
import app.domain.models.Product;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

/**
 * End-to-end security test over the real Spring Security filter chain:
 * credentials are verified, a JWT is issued, and the token unlocks a protected
 * endpoint while anonymous and wrong-password attempts are rejected.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class JwtSecurityIntegrationTest {

    private static final String RAW_PASSWORD = "SuperSecret123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BuyerRepositoryPort buyerRepository;

    @Autowired
    private UserCredentialJpaRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ProductRepositoryPort productRepository;

    @Test
    void loginIssuesTokenThatUnlocksProtectedEndpoint() throws Exception {
        Buyer buyer = seedBuyerWithCredential();
        seedPublishedProduct();

        String token = login(buyer.getEmail(), RAW_PASSWORD);
        assertNotNull(token);
        assertFalse(token.isBlank());

        // Protected endpoint rejects anonymous access.
        mockMvc.perform(get("/api/v1/products/{id}", "product-1"))
                .andExpect(status().isUnauthorized());

        // ...and accepts the request carrying the freshly issued token.
        mockMvc.perform(get("/api/v1/products/{id}", "product-1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("product-1"));
    }

    @Test
    void wrongPasswordIsRejectedWithUnauthorized() throws Exception {
        Buyer buyer = seedBuyerWithCredential();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + buyer.getEmail() + "\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blockedUserCannotLogInEvenWithCorrectPassword() throws Exception {
        Buyer buyer = seedBuyerWithCredential();
        buyer.setStatus(UserStatus.BLOCKED);
        buyerRepository.update(buyer);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + buyer.getEmail() + "\",\"password\":\"" + RAW_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private Buyer seedBuyerWithCredential() {
        Buyer buyer = new Buyer();
        buyer.setIdentifier(UUID.randomUUID().toString());
        buyer.setFullName("Secured Buyer");
        buyer.setEmail(buyer.getIdentifier() + "@example.test");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setBuyerStatus(BuyerStatus.ENABLED);
        buyer.setPrimaryAddress("Address");
        Buyer saved = buyerRepository.save(buyer);

        UserCredentialJpaEntity credential = new UserCredentialJpaEntity();
        credential.setUserIdentifier(saved.getIdentifier());
        credential.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        credentialRepository.save(credential);
        return saved;
    }

    private void seedPublishedProduct() {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setPrice(new BigDecimal("10.00"));
        product.setStatus(ProductStatus.PUBLISHED);
        product.setDigitalAsset("asset-ref");
        when(productRepository.findByIdentifier(any(Product.class))).thenReturn(Optional.of(product));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}

