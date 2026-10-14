package application.adapters.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import application.adapters.rest.controllers.ProductController;
import application.adapters.rest.mappers.ProductRestMapper;
import application.adapters.useCases.product.ProductService;
import application.domain.exceptions.DomainValidationException;
import application.domain.models.DigitalProduct;
import application.domain.models.Product;
import application.domain.valueobjects.ProductStatus;

@WebMvcTest(ProductController.class)
@Import(ProductRestMapper.class)
class ProductControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser
    void authenticatedUserCanConsultPublishedProduct() throws Exception {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-123");
        product.setStatus(ProductStatus.PUBLISHED);
        product.setDigitalAsset("asset-123");
        when(productService.consultPublished(any(Product.class))).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/{identifier}", "product-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("product-123"))
                .andExpect(jsonPath("$.type").value("DIGITAL"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void unauthenticatedUserCannotConsultProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/{identifier}", "product-123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void hiddenProductIsReturnedAsNotFound() throws Exception {
        when(productService.consultPublished(any(Product.class)))
                .thenThrow(new DomainValidationException("Product not found"));

        mockMvc.perform(get("/api/v1/products/{identifier}", "product-123"))
                .andExpect(status().isNotFound());
    }
}