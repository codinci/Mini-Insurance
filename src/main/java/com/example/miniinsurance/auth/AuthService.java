package com.example.miniinsurance.auth;

import com.example.miniinsurance.auth.dto.RegisterRequest;
import com.example.miniinsurance.user.Role;
import com.example.miniinsurance.user.User;
import com.example.miniinsurance.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);
    }
}