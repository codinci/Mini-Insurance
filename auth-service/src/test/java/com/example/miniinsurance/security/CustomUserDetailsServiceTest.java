package com.example.miniinsurance.security;

import com.example.miniinsurance.user.Role;
import com.example.miniinsurance.user.User;
import com.example.miniinsurance.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void loadsCustomerWithRolePrefixedAuthority() {
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(
                User.builder().email("ana@example.com").password("hash").role(Role.CUSTOMER).build()));

        UserDetails details = service.loadUserByUsername("ana@example.com");

        assertThat(details.getUsername()).isEqualTo("ana@example.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void loadsAdminWithAdminAuthority() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(
                User.builder().email("admin@example.com").password("hash").role(Role.ADMIN).build()));

        UserDetails details = service.loadUserByUsername("admin@example.com");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void unknownUserThrowsUsernameNotFound() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
