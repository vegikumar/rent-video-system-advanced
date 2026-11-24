package com.crio.rentvideo.service;

import com.crio.rentvideo.dto.AuthResponse;
import com.crio.rentvideo.dto.LoginRequest;
import com.crio.rentvideo.dto.RegisterRequest;
import com.crio.rentvideo.dto.UserResponse;
import com.crio.rentvideo.entity.Role;
import com.crio.rentvideo.entity.User;
import com.crio.rentvideo.exception.BadRequestException;
import com.crio.rentvideo.exception.UnauthorizedException;
import com.crio.rentvideo.repository.UserRepository;
import com.crio.rentvideo.security.CustomUserDetails;
import com.crio.rentvideo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.CUSTOMER;

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(role)
                .build();

        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        return authenticateAndIssueToken(request.getEmail(), request.getPassword());
    }

    public AuthResponse loginWithBasicAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Basic Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new UnauthorizedException("Invalid authentication principal");
        }

        String token = jwtService.generateToken(userDetails);
        User user = userDetails.getUser();
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    private AuthResponse authenticateAndIssueToken(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email.trim().toLowerCase(), password));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        User user = userDetails.getUser();

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }
}
