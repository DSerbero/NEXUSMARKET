package app.domain.ports.out;

import java.util.Optional;

import app.domain.models.Refund;

public interface RefundRepositoryPort {
    Refund save(Refund refund);
    Optional<Refund> findByReturn(Refund refund);
    Optional<Refund> findByIdentifier(Refund refund);
    Refund update(Refund refund);
}
