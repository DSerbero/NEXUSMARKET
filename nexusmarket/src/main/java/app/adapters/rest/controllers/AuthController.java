package app.adapters.rest.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.adapters.rest.dtos.requests.LoginRequest;
import app.adapters.rest.dtos.responses.LoginResponse;
import app.domain.ports.in.AuthenticationPort;
import app.domain.ports.in.LoginResult;
import lombok.RequiredArgsConstructor;

/**
 * Exposes authentication over HTTP. The controller only maps the request to the
 * {@link AuthenticationPort} and formats the {@link LoginResult}; token minting
 * and business rules live in the application/domain layers. Failures such as
 * invalid credentials are translated by the global {@code RestExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationPort authenticationPort;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        LoginResult result = authenticationPort.login(request.email(), request.password());
        return LoginResponse.bearer(result.token(), result.user().getIdentifier(), result.user().getRole().getCode());
    }
}
