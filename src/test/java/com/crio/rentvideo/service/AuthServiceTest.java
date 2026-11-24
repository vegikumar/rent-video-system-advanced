package com.crio.rentvideo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crio.rentvideo.dto.RegisterRequest;
import com.crio.rentvideo.dto.UserResponse;
import com.crio.rentvideo.entity.Role;
import com.crio.rentvideo.entity.User;
import com.crio.rentvideo.exception.BadRequestException;
import com.crio.rentvideo.repository.UserRepository;
import com.crio.rentvideo.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_defaultsRoleToCustomer() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@example.com")
                .password("secret1")
                .firstName("New")
                .lastName("User")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = authService.register(request);

        assertEquals(Role.CUSTOMER, response.getRole());
        assertEquals("new@example.com", response.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hashed", captor.getValue().getPassword());
        assertEquals(Role.CUSTOMER, captor.getValue().getRole());
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("dup@example.com")
                .password("secret1")
                .firstName("Dup")
                .lastName("User")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void register_allowsAdminRoleWhenSpecified() {
        RegisterRequest request = RegisterRequest.builder()
                .email("admin@example.com")
                .password("secret1")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        UserResponse response = authService.register(request);
        assertEquals(Role.ADMIN, response.getRole());
    }
}
