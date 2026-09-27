package app.domain.services.ports;

import app.domain.models.User;

public interface SecurityPort {
    User authenticate(String email, String password);
    void logout(User user);
}
