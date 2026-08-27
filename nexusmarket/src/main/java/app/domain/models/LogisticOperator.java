package app.domain.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class LogisticOperator extends User {
    private List<Warehouse> assignedWarehouse = new ArrayList<>();
}