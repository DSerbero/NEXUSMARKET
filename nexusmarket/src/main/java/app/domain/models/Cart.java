package app.domain.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cart {
    private Buyer buyer;
    private List<Product> items = new ArrayList<>();
}
