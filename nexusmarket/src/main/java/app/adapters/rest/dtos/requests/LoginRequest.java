package app.adapters.rest.dtos.requests;

/**
 * Transport contract for the login endpoint. Credentials are primitives by
 * necessity (no authenticated user exists yet), matching the SDD.
 */
public record LoginRequest(String email, String password) {
}
