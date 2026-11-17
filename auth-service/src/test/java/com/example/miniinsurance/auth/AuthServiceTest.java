package com.example.miniinsurance.auth;

import com.example.miniinsurance.auth.dto.AuthResponse;
import com.example.miniinsurance.auth.dto.LoginRequest;
import com.example.miniinsurance.auth.dto.RegisterRequest;
import com.example.miniinsurance.common.exception.ConflictException;
import com.example.miniinsurance.security.JwtService;
import com.example.miniinsurance.user.Role;
import com.example.miniinsurance.user.User;
import com.example.miniinsurance.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String EMAIL = "ana@example.com";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtService jwtService = mock(JwtService.class);

    private final AuthService service = new AuthService(
            userRepository, passwordEncoder, authenticationManager, jwtService);

    private final RegisterRequest registerRequest =
            new RegisterRequest("Ana", "Silva", EMAIL, "secret123");

    @Test
    void register_storesCustomerWithEncodedPassword() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");

        service.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getFirstName()).isEqualTo("Ana");
        assertThat(saved.getLastName()).isEqualTo("Silva");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void register_rejectsDuplicateEmailWithConflict() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.register(registerRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokenAndUserSummary() {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(
                        EMAIL, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        User user = User.builder()
                .id(7L)
                .email(EMAIL)
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(EMAIL)).thenReturn("jwt-token");

        AuthResponse response = service.login(new LoginRequest(EMAIL, "secret123"));

        assertThat(response).isEqualTo(new AuthResponse("jwt-token", 7L, EMAIL, "CUSTOMER"));
    }

    @Test
    void login_badCredentialsDoNotIssueToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}
