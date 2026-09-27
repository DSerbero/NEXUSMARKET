package app.domain.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import app.domain.valueObjects.ProductStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class Product {
    private BigDecimal price;
    private List<String> variants = new ArrayList<>();
    private ProductStatus status;
    private Seller seller;
}
