package app.domain.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import app.domain.valueobjects.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Order {
    private String identifier;
    private Buyer buyer;
    private List<OrderItemSnapshot> items = new ArrayList<>();
    private OrderStatus orderStatus;
    private LocalDateTime creationDate;
    private LocalDateTime completionDate;
}
