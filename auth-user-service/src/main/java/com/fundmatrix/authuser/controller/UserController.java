package com.fundmatrix.authuser.controller;

import com.fundmatrix.authuser.domain.enums.Role;
import com.fundmatrix.authuser.dto.CreateUserRequest;
import com.fundmatrix.authuser.dto.UpdateUserStatusRequest;
import com.fundmatrix.authuser.dto.UserDto;
import com.fundmatrix.authuser.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Administrative user management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> list(@RequestParam(required = false) Role role) {
        return userService.list(role);
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.get(id);
    }

    @GetMapping("/exists/{id}")
    public Boolean exists(@PathVariable Long id) {
        return userService.exists(id);
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.create(request));
    }

    @PatchMapping("/{id}/status")
    public UserDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateStatus(id, request.status());
    }
}
