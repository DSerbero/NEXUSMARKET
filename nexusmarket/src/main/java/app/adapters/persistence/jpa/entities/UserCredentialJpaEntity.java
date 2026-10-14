package app.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores the password hash for a {@link UserJpaEntity}. Passwords never belong
 * to the domain model; they live only in this security boundary table
 * ({@code user_credentials}), created in {@code V1__checkout_persistence.sql}.
 */
@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
public class UserCredentialJpaEntity {

    @Id
    @Column(name = "user_identifier", nullable = false, length = 36)
    private String userIdentifier;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
}
