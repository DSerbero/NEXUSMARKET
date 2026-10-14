package app.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;


@Getter
@Setter
@NoArgsConstructor
public class User {
    private String identifier;
    private String fullName;
    private String email;
    private UserRole role;
    private UserStatus status;
}
