package app.domain.services.ports;

import java.util.List;
import java.util.Optional;

import app.domain.models.Shipment;

public interface ShipmentRepositoryPort {
    Shipment save(Shipment shipment);
    Optional<Shipment> findByOrder(Shipment shipment);
    Optional<Shipment> findByIdentifier(Shipment shipment);
    List<Shipment> findByOperator(Shipment shipment);
    Shipment update(Shipment shipment);
}
