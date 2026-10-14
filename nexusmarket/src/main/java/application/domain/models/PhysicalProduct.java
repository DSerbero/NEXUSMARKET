package application.domain.models;

import lombok.Getter;

@Getter
public class PhysicalProduct extends Product{
    private Inventory associatedInventory;

    
}
