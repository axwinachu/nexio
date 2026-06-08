package com.nexio.nexio.user.controller;

import com.nexio.nexio.user.dto.UpdateUserRequest;
import com.nexio.nexio.user.dto.UserResponse;
import com.nexio.nexio.user.facade.UserFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile management")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {
    private final UserFacade userFacade;

    private static final String USER_ID_HEADER="X-User-Id";
    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public UserResponse getProfile(HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
        return userFacade.getProfile(userId);
    }

    @Operation(summary = "Update current user profile")
    @PutMapping("/me")
    public UserResponse updateProfile(HttpServletRequest request, @Valid @RequestBody UpdateUserRequest body){
        Long userId = (Long) request.getAttribute("userId");
        return userFacade.updateProfile(userId,body);
    }

    @Operation(summary = "Delete current user account")
    @DeleteMapping("/me")
    public void deleteAccount(HttpServletRequest request){
        Long userId = (Long) request.getAttribute("userId");
        userFacade.deleteAccount(userId);
    }

}
