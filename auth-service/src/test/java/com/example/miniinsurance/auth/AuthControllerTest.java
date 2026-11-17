package com.example.miniinsurance.auth;

import com.example.miniinsurance.auth.dto.AuthResponse;
import com.example.miniinsurance.common.ApiExceptionHandler;
import com.example.miniinsurance.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for the public auth endpoints. Standalone MockMvc: no database, no security filter chain.
 */
class AuthControllerTest {

    private final AuthService service = mock(AuthService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void register_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Silva",
                                 "email":"ana@example.com","password":"secret123"}"""))
                .andExpect(status().isCreated());

        verify(service).register(any());
    }

    @Test
    void register_invalidEmail_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Silva",
                                 "email":"not-an-email","password":"secret123"}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Silva",
                                 "email":"ana@example.com","password":"short"}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        doThrow(new ConflictException("Email is already registered"))
                .when(service).register(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Silva",
                                 "email":"ana@example.com","password":"secret123"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void login_returns200WithToken() throws Exception {
        when(service.login(any())).thenReturn(
                new AuthResponse("jwt-token", 7L, "ana@example.com", "CUSTOMER"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"secret123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        when(service.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"wrong-password"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }
}
