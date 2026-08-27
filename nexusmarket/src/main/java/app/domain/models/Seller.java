package app.domain.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class Seller extends User {
    private List<Warehouse> associatedWarehouse = new ArrayList<>();
    private List<Product> productCatalog = new ArrayList<>();
}