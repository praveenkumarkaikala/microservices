package com.fundmatrix.dashboard.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** Read-only consumer of auth-user-service (owns User). */
@FeignClient(name = "auth-user-service", path = "/api")
public interface AuthUserClient {

    /** FEIGN_CONTRACTS.md internal contract. */
    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable Long id);

    /** Existing monolith-derived admin listing route (UserController.list) - used for total-user counts. */
    @GetMapping("/users")
    List<UserDto> listUsers(@RequestParam(required = false) String role);
}
