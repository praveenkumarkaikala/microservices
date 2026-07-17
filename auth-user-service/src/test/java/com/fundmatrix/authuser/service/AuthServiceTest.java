package com.fundmatrix.authuser.service;

import com.fundmatrix.authuser.common.exception.BusinessException;
import com.fundmatrix.authuser.domain.User;
import com.fundmatrix.authuser.domain.enums.Role;
import com.fundmatrix.authuser.domain.enums.UserStatus;
import com.fundmatrix.authuser.dto.AuthResponse;
import com.fundmatrix.authuser.dto.LoginRequest;
import com.fundmatrix.authuser.dto.RegisterRequest;
import com.fundmatrix.authuser.repository.UserRepository;
import com.fundmatrix.authuser.security.CurrentUserService;
import com.fundmatrix.authuser.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CurrentUserService currentUser;

    private final Mapper mapper = new Mapper();

    @Test
    void register_success_returnsTokenAndUser() {
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, currentUser, mapper);
        RegisterRequest req = new RegisterRequest("Jane Doe", "Jane@Example.com", "9999999999", "secret1");

        when(userRepository.existsByEmailIgnoreCase("Jane@Example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            u.setCreatedAt(Instant.now());
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("token-abc");
        when(jwtService.expiryFromNow()).thenReturn(Instant.now().plusSeconds(3600));

        AuthResponse response = service.register(req);

        assertEquals("token-abc", response.token());
        assertEquals("jane@example.com", response.user().email());
        assertEquals(Role.INVESTOR, response.user().role());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void register_duplicateEmail_throwsBusinessException() {
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, currentUser, mapper);
        RegisterRequest req = new RegisterRequest("Jane Doe", "jane@example.com", "9999999999", "secret1");

        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.register(req));
    }

    @Test
    void login_success_returnsToken() {
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, currentUser, mapper);
        LoginRequest req = new LoginRequest("jane@example.com", "secret1");

        User user = User.builder()
                .name("Jane Doe").email("jane@example.com").phone("9999999999")
                .role(Role.INVESTOR).status(UserStatus.ACTIVE).password("hashed")
                .build();
        user.setId(1L);

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret1", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token-xyz");
        when(jwtService.expiryFromNow()).thenReturn(Instant.now().plusSeconds(3600));

        AuthResponse response = service.login(req);

        assertEquals("token-xyz", response.token());
        assertEquals("jane@example.com", response.user().email());
    }

    @Test
    void login_badPassword_throwsBusinessException() {
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, currentUser, mapper);
        LoginRequest req = new LoginRequest("jane@example.com", "wrongpass");

        User user = User.builder()
                .name("Jane Doe").email("jane@example.com")
                .role(Role.INVESTOR).status(UserStatus.ACTIVE).password("hashed")
                .build();
        user.setId(1L);

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.login(req));
    }
}
