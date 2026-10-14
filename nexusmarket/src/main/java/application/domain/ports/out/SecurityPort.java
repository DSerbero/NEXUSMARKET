package application.domain.ports.out;

import application.domain.models.User;

public interface SecurityPort {
    User authenticate(String email, String password);
    void logout(User user);
}
