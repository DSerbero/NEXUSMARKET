package app.adapters.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(
        name = "uk_users_normalized_email", columnNames = "normalized_email"))
@Getter
@Setter
@NoArgsConstructor
public class UserJpaEntity {

    @Id
    @Column(name = "identifier", nullable = false, length = 36)
    private String identifier;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "normalized_email", nullable = false, length = 320)
    private String normalizedEmail;

    @Column(name = "role_code", nullable = false, length = 40)
    private String roleCode;

    @Column(name = "status_code", nullable = false, length = 40)
    private String statusCode;
}