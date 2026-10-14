package app.domain.ports.in;

import app.domain.models.User;

/**
 * Input port for user authentication. Login is the single legitimate exception
 * to the "no primitives" rule: at that point no authenticated {@link User}
 * exists yet, so raw credentials are received from the outside.
 */
public interface AuthenticationPort {

    User login(String email, String password);
}
