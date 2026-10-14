package app.domain.ports.out;

import app.domain.models.User;

public interface SecurityPort {
    User authenticate(String email, String password);
    void logout(User user);
}
