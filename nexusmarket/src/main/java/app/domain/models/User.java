package app.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import app.domain.valueObjects.UserRole;
import app.domain.valueObjects.UserStatus;


@Getter
@Setter
@NoArgsConstructor
public class User {
    private long identifier;
    private String fullName;
    private String email;
    private UserRole role;
    private UserStatus status;
}
