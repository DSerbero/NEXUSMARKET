package app.domain.ports.in;

import app.domain.models.User;

/**
 * Outcome of a successful login: the authenticated user plus the access token
 * minted by the security adapter. Token orchestration lives in the application
 * (use-case) layer, so the REST adapter never talks to an output port.
 */
public record LoginResult(User user, String token) {
}