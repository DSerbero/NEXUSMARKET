package application.domain.ports.out;

import java.util.List;
import java.util.Optional;

import application.domain.models.Warehouse;

public interface WarehouseRepositoryPort {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findByIdentifier(Warehouse warehouse);
    List<Warehouse> findByOwner(Warehouse warehouse);
    Warehouse update(Warehouse warehouse);
}
