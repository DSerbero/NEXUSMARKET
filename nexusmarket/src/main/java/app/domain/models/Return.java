package app.domain.models;

import java.time.LocalDateTime;

import app.domain.valueObjects.ReturnStatus;
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
