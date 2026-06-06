package com.nexio.nexio.user.mapper;

import com.nexio.nexio.user.dto.UserResponse;
import com.nexio.nexio.user.model.User;

public class UserMapper {
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

}
