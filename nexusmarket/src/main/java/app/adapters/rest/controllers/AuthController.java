package app.adapters.rest.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.adapters.rest.dtos.requests.LoginRequest;
import app.adapters.rest.dtos.responses.LoginResponse;
import app.domain.exceptions.DomainValidationException;
import app.domain.models.User;
import app.domain.ports.in.AuthenticationPort;
import app.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;

/**
 * Exposes authentication over HTTP. It coordinates the domain login
 * ({@link AuthenticationPort}) with token minting ({@link JwtService}); the
 * controller itself applies no business rules.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationPort authenticationPort;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        User user = authenticationPort.login(request.email(), request.password());
        String token = jwtService.generateToken(user);
        return LoginResponse.bearer(token, user.getIdentifier(), user.getRole().getCode());
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<Void> handleInvalidCredentials() {
        return ResponseEntity.status(401).build();
    }
}
