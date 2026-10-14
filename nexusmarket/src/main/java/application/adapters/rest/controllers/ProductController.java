package application.adapters.rest.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import application.adapters.rest.dtos.responses.ProductResponse;
import application.adapters.rest.mappers.ProductRestMapper;
import application.adapters.useCases.product.ProductService;
import application.domain.exceptions.DomainValidationException;
import application.domain.models.PhysicalProduct;
import application.domain.models.Product;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductRestMapper productRestMapper;

    @GetMapping("/{identifier}")
    public ProductResponse consultPublished(@PathVariable String identifier) {
        Product query = new PhysicalProduct();
        query.setIdentifier(identifier);
        return productRestMapper.toResponse(productService.consultPublished(query));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<Void> handleUnavailableProduct() {
        return ResponseEntity.notFound().build();
    }
}