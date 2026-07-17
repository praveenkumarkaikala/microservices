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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuditService auditService, CurrentUserService currentUser, Mapper mapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessException("An account with this email already exists");
        }
        User user = User.builder()
                .name(req.name())
                .email(req.email().toLowerCase())
                .phone(req.phone())
                .role(req.role())
                .status(UserStatus.ACTIVE)
                .password(passwordEncoder.encode(req.password()))
                .build();
        user = userRepository.save(user);
        auditService.record("USER_CREATE", "User", user.getId(),
                "Created " + req.role() + " account " + user.getEmail());
        return mapper.toUserDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> list(Role role) {
        List<User> users = (role == null) ? userRepository.findAll() : userRepository.findByRole(role);
        return users.stream().map(mapper::toUserDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return userRepository.findById(id).map(mapper::toUserDto)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return userRepository.existsById(id);
    }

    @Transactional
    public UserDto updateStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
        if (user.getId().equals(currentUser.getId()) && status != UserStatus.ACTIVE) {
            throw new BusinessException("You cannot deactivate your own account");
        }
        user.setStatus(status);
        auditService.record("USER_STATUS", "User", id, "Status set to " + status);
        return mapper.toUserDto(userRepository.save(user));
    }
}
