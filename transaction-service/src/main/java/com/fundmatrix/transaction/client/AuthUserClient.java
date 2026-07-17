package com.fundmatrix.transaction.client;

import com.fundmatrix.transaction.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-user-service", path = "/api")
public interface AuthUserClient {

    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable Long id);
}
