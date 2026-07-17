package com.fundmatrix.authuser.service;

import com.fundmatrix.authuser.common.exception.BusinessException;
import com.fundmatrix.authuser.domain.User;
import com.fundmatrix.authuser.domain.enums.Role;
import com.fundmatrix.authuser.domain.enums.UserStatus;
import com.fundmatrix.authuser.dto.AuthResponse;
import com.fundmatrix.authuser.dto.LoginRequest;
import com.fundmatrix.authuser.dto.RegisterRequest;
import com.fundmatrix.authuser.dto.UserDto;
import com.fundmatrix.authuser.repository.UserRepository;
import com.fundmatrix.authuser.security.CurrentUserService;
import com.fundmatrix.authuser.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CurrentUserService currentUser, Mapper mapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessException("An account with this email already exists");
        }
        User user = User.builder()
                .name(req.name())
                .email(req.email().toLowerCase())
                .phone(req.phone())
                .role(Role.INVESTOR)
                .status(UserStatus.ACTIVE)
                .password(passwordEncoder.encode(req.password()))
                .build();
        return issueToken(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new BusinessException("Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is " + user.getStatus());
        }
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public UserDto me() {
        return mapper.toUserDto(currentUser.requireUser());
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtService.expiryFromNow(), mapper.toUserDto(user));
    }
}
