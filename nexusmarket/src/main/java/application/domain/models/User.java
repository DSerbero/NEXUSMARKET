package application.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;


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
