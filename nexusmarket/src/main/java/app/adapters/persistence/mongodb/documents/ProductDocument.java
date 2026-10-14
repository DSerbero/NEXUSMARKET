package app.adapters.persistence.mongodb.documents;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductDocument {

    @Id
    private String identifier;
    private String productType;
    private BigDecimal price;
    private List<String> variants = new ArrayList<>();
    private String status;
    private String sellerIdentifier;
    private String digitalAsset;
}