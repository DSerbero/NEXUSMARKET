package app.adapters.rest.dtos.responses;

import java.time.Instant;

/**
 * Standard error envelope returned by the REST layer for any handled failure.
 * Keeps error responses consistent across controllers.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message) {

    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(Instant.now(), status, error, message);
    }
}
