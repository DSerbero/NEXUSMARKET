package app.adapters.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import app.adapters.rest.dtos.responses.ApiErrorResponse;
import app.domain.exceptions.DomainValidationException;
import app.domain.exceptions.EntityNotFoundException;
import app.domain.exceptions.InvalidCredentialsException;
import app.domain.exceptions.UnauthorizedOperationException;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void entityNotFoundMapsTo404() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleNotFound(new EntityNotFoundException("Product"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertEquals("Product not found", response.getBody().message());
    }

    @Test
    void invalidCredentialsMapsTo401() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleInvalidCredentials(new InvalidCredentialsException());

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
    }

    @Test
    void unauthorizedOperationMapsTo403() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnauthorized(new UnauthorizedOperationException("nope"));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatusCode().value());
    }

    @Test
    void domainValidationMapsTo400() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleDomainValidation(new DomainValidationException("bad input"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertEquals("bad input", response.getBody().message());
    }
}
