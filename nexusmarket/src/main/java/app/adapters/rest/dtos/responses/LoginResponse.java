package app.adapters.rest.dtos.responses;

/**
 * Returned by the login endpoint. The access token is produced by the security
 * adapter and is intentionally a REST-level concern, never a domain value.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        String identifier,
        String role) {

    public static LoginResponse bearer(String accessToken, String identifier, String role) {
        return new LoginResponse(accessToken, "Bearer", identifier, role);
    }
}
