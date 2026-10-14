package app.domain.valueobjects;

/**
 * Immutable snapshot of the claims the domain needs from a verified access
 * token: who the caller is and which role they hold. The actual JWT mechanics
 * (signing, parsing, library types) stay behind {@code JwtServicePort}; this
 * type keeps JJWT out of the domain (SDD).
 */
public record TokenClaims(String subject, String role) {
}