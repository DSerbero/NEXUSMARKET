package app.adapters.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import app.adapters.rest.controllers.AuthController;
import app.domain.exceptions.InvalidCredentialsException;
import app.domain.models.User;
import app.domain.ports.in.AuthenticationPort;
import app.domain.ports.in.LoginResult;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

/**
 * Controller slice test. Security filters are disabled so this test focuses on
 * request/response mapping and the 401 handling; end-to-end protection is
 * covered by the integration test.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationPort authenticationPort;

    @Test
    void validLoginReturnsAccessToken() throws Exception {
        User user = new User();
        user.setIdentifier("user-1");
        user.setRole(UserRole.BUYER);
        user.setStatus(UserStatus.ACTIVE);
        when(authenticationPort.login("user@example.test", "secret"))
                .thenReturn(new LoginResult(user, "header.payload.signature"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.test\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("header.payload.signature"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.identifier").value("user-1"))
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void invalidCredentialsReturnUnauthorized() throws Exception {
        when(authenticationPort.login(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.test\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequestIsBoundFromJson() throws Exception {
        // Sanity check that the record DTO is deserialized (email/password wired).
        when(authenticationPort.login("a@b.test", "p")).thenThrow(new InvalidCredentialsException());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.test\",\"password\":\"p\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankEmailIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("email is required"));
    }

    @Test
    void malformedEmailIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email must be valid"));
    }

    @Test
    void blankPasswordIsRejectedWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.test\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password is required"));
    }
}
