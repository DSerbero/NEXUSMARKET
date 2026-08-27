package app.domain.models;

import java.util.ArrayList;
import java.util.List;

import app.domain.valueObjects.ProductStatus;
import app.domain.valueObjects.ProductType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class Product {
    private ProductType productType;
    private List<String> variants = new ArrayList<>();
    private ProductStatus status;
    private Seller seller;
}
