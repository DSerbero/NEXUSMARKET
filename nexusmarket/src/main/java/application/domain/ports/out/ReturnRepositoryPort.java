package application.domain.ports.out;

import java.util.Optional;

import application.domain.models.Return;

public interface ReturnRepositoryPort {
    Return save(Return returnRequest);
    Optional<Return> findByIdentifier(Return returnRequest);
    Optional<Return> findActiveByOrder(Return returnRequest);
    Return update(Return returnRequest);
}
