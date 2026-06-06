package com.nexio.nexio.user.controller;

import com.nexio.nexio.user.dto.UpdateUserRequest;
import com.nexio.nexio.user.dto.UserResponse;
import com.nexio.nexio.user.facade.UserFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserFacade userFacade;

    private static final String USER_ID_HEADER="X-User-Id";

    @GetMapping("/me")
    public UserResponse getProfile(@RequestHeader(USER_ID_HEADER) Long userId){
        return userFacade.getProfile(userId);
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@RequestHeader(USER_ID_HEADER) Long userId, @Valid @RequestBody UpdateUserRequest request){
        return userFacade.updateProfile(userId,request);
    }

    @DeleteMapping("/me")
    public void deleteAccount(@RequestHeader(USER_ID_HEADER) Long userId){
        userFacade.deleteAccount(userId);
    }

}
