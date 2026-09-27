package app.domain.services.ports;

import java.util.List;
import java.util.Optional;

import app.domain.models.Warehouse;

public interface WarehouseRepositoryPort {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findByIdentifier(Warehouse warehouse);
    List<Warehouse> findByOwner(Warehouse warehouse);
    Warehouse update(Warehouse warehouse);
}
