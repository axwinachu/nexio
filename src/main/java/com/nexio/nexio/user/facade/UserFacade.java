package com.nexio.nexio.user.facade;

import com.nexio.nexio.user.dto.UpdateUserRequest;
import com.nexio.nexio.user.dto.UserResponse;
import com.nexio.nexio.user.mapper.UserMapper;
import com.nexio.nexio.user.model.User;
import com.nexio.nexio.user.service.UserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacade {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    public UserResponse getProfile(Long userId) {
        User user=findById(userId);
        return userMapper.toResponse(user);
    }
    @Transactional
    public UserResponse updateProfile(Long userId, @Valid UpdateUserRequest request) {
        User user=findById(userId);
        user.setName(request.getName());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userMapper.toResponse(userService.save(user));
    }
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findById(userId);
        userService.delete(user);
    }

    private User findById(Long userId){
        return userService.findById(userId).orElseThrow(()->new RuntimeException("User Not found in the userId:"+userId));
    }
}
