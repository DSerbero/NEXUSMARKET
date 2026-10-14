package app.adapters.rest.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.adapters.rest.dtos.responses.ProductResponse;
import app.adapters.rest.mappers.ProductRestMapper;
import app.domain.ports.in.PublicAccessPort;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.PhysicalProduct;
import app.domain.models.Product;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final PublicAccessPort publicAccessPort;
    private final ProductRestMapper productRestMapper;

    @GetMapping("/{identifier}")
    public ProductResponse consultPublished(@PathVariable String identifier) {
        Product query = new PhysicalProduct();
        query.setIdentifier(identifier);
        return productRestMapper.toResponse(publicAccessPort.consultPublishedProduct(query));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<Void> handleUnavailableProduct() {
        return ResponseEntity.notFound().build();
    }
}