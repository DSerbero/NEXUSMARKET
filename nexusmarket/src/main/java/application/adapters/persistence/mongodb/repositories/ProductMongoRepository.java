package application.adapters.persistence.mongodb.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import application.adapters.persistence.mongodb.documents.ProductDocument;

public interface ProductMongoRepository extends MongoRepository<ProductDocument, String> {

    List<ProductDocument> findAllBySellerIdentifier(String sellerIdentifier);
}