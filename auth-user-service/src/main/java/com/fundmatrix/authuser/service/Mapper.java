package com.fundmatrix.authuser.service;

import com.fundmatrix.authuser.domain.User;
import com.fundmatrix.authuser.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class Mapper {

    public UserDto toUserDto(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getStatus(), u.getCreatedAt());
    }
}
