package com.fundmatrix.authuser.service;

import com.fundmatrix.authuser.common.exception.BusinessException;
import com.fundmatrix.authuser.common.exception.ResourceNotFoundException;
import com.fundmatrix.authuser.domain.User;
import com.fundmatrix.authuser.domain.enums.Role;
import com.fundmatrix.authuser.domain.enums.UserStatus;
import com.fundmatrix.authuser.dto.CreateUserRequest;
import com.fundmatrix.authuser.dto.UserDto;
import com.fundmatrix.authuser.repository.UserRepository;
import com.fundmatrix.authuser.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private CurrentUserService currentUser;

    private final Mapper mapper = new Mapper();

    private UserService service() {
        return new UserService(userRepository, passwordEncoder, auditService, currentUser, mapper);
    }

    @Test
    void create_success_returnsUserDto() {
        UserService userService = service();
        CreateUserRequest req = new CreateUserRequest("Amit Kumar", "Amit@Example.com", "8888888888",
                Role.DISTRIBUTOR, "secret1");

        when(userRepository.existsByEmailIgnoreCase("Amit@Example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserDto dto = userService.create(req);

        assertEquals("amit@example.com", dto.email());
        assertEquals(Role.DISTRIBUTOR, dto.role());
        assertEquals(UserStatus.ACTIVE, dto.status());
    }

    @Test
    void create_duplicateEmail_throwsBusinessException() {
        UserService userService = service();
        CreateUserRequest req = new CreateUserRequest("Amit Kumar", "amit@example.com", "8888888888",
                Role.DISTRIBUTOR, "secret1");

        when(userRepository.existsByEmailIgnoreCase("amit@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.create(req));
    }

    @Test
    void updateStatus_success_updatesAndReturnsDto() {
        UserService userService = service();
        User user = User.builder().name("Test").email("test@example.com")
                .role(Role.INVESTOR).status(UserStatus.ACTIVE).password("hashed").build();
        user.setId(5L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(currentUser.getId()).thenReturn(99L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto dto = userService.updateStatus(5L, UserStatus.SUSPENDED);

        assertEquals(UserStatus.SUSPENDED, dto.status());
    }

    @Test
    void updateStatus_userNotFound_throwsResourceNotFoundException() {
        UserService userService = service();
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateStatus(42L, UserStatus.SUSPENDED));
    }

    @Test
    void get_userNotFound_throwsResourceNotFoundException() {
        UserService userService = service();
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.get(7L));
    }
}
