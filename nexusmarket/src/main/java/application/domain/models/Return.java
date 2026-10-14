package application.domain.models;

import java.time.LocalDateTime;

import application.domain.valueobjects.ReturnStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Return {
    private Order order;
    private String reason;
    private ReturnStatus returnStatus;
    private LocalDateTime requestDate;
}
