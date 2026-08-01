package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.LoginRequest;
import com.project.estate.dto.request.RefreshTokenRequest;
import com.project.estate.dto.request.UserRequest;
import com.project.estate.dto.response.TokenResponse;
import com.project.estate.dto.response.UserResponse;
import com.project.estate.service.AuthService;
import com.project.estate.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.success(authService.getMyInfo());
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.success(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody UserRequest userRequest) {
        return ApiResponse.success(userService.createUser(userRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refreshToken(request));
    }

    @DeleteMapping("/logout")
    public ApiResponse<Void> logout (@RequestHeader ("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ApiResponse.success();
    }

    @GetMapping("/verify")
    public ApiResponse<Void> confirmEmail(@RequestParam String token)  {
        authService.confirmEmail(token);
        return ApiResponse.success();
    }
}

