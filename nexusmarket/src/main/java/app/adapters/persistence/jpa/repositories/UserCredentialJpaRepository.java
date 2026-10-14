package app.adapters.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import app.adapters.persistence.jpa.entities.UserCredentialJpaEntity;

public interface UserCredentialJpaRepository extends JpaRepository<UserCredentialJpaEntity, String> {
}
