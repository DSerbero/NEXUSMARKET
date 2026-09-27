package app.domain.services.ports;

import java.util.Optional;

import app.domain.models.Return;

public interface ReturnRepositoryPort {
    Return save(Return returnRequest);
    Optional<Return> findByIdentifier(Return returnRequest);
    Optional<Return> findActiveByOrder(Return returnRequest);
    Return update(Return returnRequest);
}
