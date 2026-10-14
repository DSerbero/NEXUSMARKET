package application.domain.models;

import application.domain.valueobjects.WarehouseOwnerType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Warehouse {
    private long identifier;
    private String location;
    private WarehouseOwnerType ownerType;
    private User owner;
    private User responsibleUser;
}
