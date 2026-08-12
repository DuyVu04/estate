package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.common.response.PageResponse;
import com.project.estate.dto.request.UserRequest;
import com.project.estate.dto.response.UserResponse;
import com.project.estate.entity.User;
import com.project.estate.service.UserService;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping
  public ApiResponse<PageResponse<UserResponse>> getUsers(
      @Filter Specification<User> specification, Pageable pageable) {
    return ApiResponse.success(PageResponse.of(userService.getUsers(specification, pageable)));
  }

  @PostMapping
  public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserRequest userRequest) {
    return ApiResponse.success(userService.createUser(userRequest));
  }

  @GetMapping("/{userId}")
  public ApiResponse<UserResponse> getUserById(@PathVariable String userId) {
    return ApiResponse.success(userService.getUserById(userId));
  }

  @DeleteMapping("/{userId}")
  public ApiResponse<Void> deleteUser(@PathVariable String userId) {
    userService.deleteUser(userId);
    return ApiResponse.success();
  }
}
