package com.nexio.nexio.user.controller;

import com.nexio.nexio.user.dto.UpdateUserRequest;
import com.nexio.nexio.user.dto.UserResponse;
import com.nexio.nexio.user.facade.UserFacade;
import jakarta.servlet.http.HttpServletRequest;
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
    public UserResponse getProfile(HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
        return userFacade.getProfile(userId);
    }

    @PutMapping("/me")
    public UserResponse updateProfile(HttpServletRequest request, @Valid @RequestBody UpdateUserRequest body){
        Long userId = (Long) request.getAttribute("userId");
        return userFacade.updateProfile(userId,body);
    }

    @DeleteMapping("/me")
    public void deleteAccount(HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
        userFacade.deleteAccount(userId);
    }

}
