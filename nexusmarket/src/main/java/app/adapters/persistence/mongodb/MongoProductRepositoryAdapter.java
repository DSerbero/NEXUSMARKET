package app.adapters.persistence.mongodb;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import app.adapters.persistence.mongodb.documents.ProductDocument;
import app.adapters.persistence.mongodb.mappers.ProductDocumentMapper;
import app.adapters.persistence.mongodb.repositories.ProductMongoRepository;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.Product;
import app.domain.ports.out.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MongoProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductMongoRepository repository;
    private final ProductDocumentMapper mapper;

    @Override
    public Product save(Product product) {
        if (product.getIdentifier() == null || product.getIdentifier().isBlank()) {
            product.setIdentifier(UUID.randomUUID().toString());
        }
        return mapper.toDomain(repository.save(mapper.toDocument(product)));
    }

    @Override
    public Optional<Product> findByIdentifier(Product product) {
        if (product == null || product.getIdentifier() == null || product.getIdentifier().isBlank()) {
            return Optional.empty();
        }
        return repository.findById(product.getIdentifier()).map(mapper::toDomain);
    }

    @Override
    public List<Product> findBySeller(Product product) {
        if (product == null || product.getSeller() == null
                || product.getSeller().getIdentifier() == null) {
            return List.of();
        }
        return repository.findAllBySellerIdentifier(product.getSeller().getIdentifier())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Product update(Product product) {
        if (product == null || product.getIdentifier() == null || product.getIdentifier().isBlank()) {
            throw new DomainValidationException("Product identifier is required");
        }
        if (!repository.existsById(product.getIdentifier())) {
            throw new DomainValidationException("Product not found");
        }
        ProductDocument document = mapper.toDocument(product);
        return mapper.toDomain(repository.save(document));
    }
}