package application.domain.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import application.domain.valueobjects.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Order {
    private Buyer buyer;
    private List<Product> items = new ArrayList<>();
    private OrderStatus orderStatus;
    private LocalDateTime creationDate;
    private LocalDateTime completionDate;
}
