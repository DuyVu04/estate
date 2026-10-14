package com.project.estate.modules.identity.controller;

import com.project.estate.modules.identity.dto.request.UserRequest;
import com.project.estate.modules.identity.dto.response.UserResponse;
import com.project.estate.modules.identity.entity.User;
import com.project.estate.modules.identity.service.UserService;
import com.project.estate.shared.response.ApiResponse;
import com.project.estate.shared.response.PageResponse;
import com.turkraft.springfilter.boot.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(
    name = "User Management",
    description = "Endpoints for querying, creating, retrieving, and deleting user accounts")
public class UserController {
  private final UserService userService;

  @GetMapping
  @Operation(
      summary = "Filter and list users",
      description =
          "Retrieves a paginated list of users with dynamic filtering using SpringFilter specification")
  public ApiResponse<PageResponse<UserResponse>> getUsers(
      @Filter Specification<User> specification, Pageable pageable) {
    return ApiResponse.success(PageResponse.of(userService.getUsers(specification, pageable)));
  }

  @PostMapping
  @Operation(
      summary = "Create user",
      description = "Creates a new user profile in the system with encrypted password")
  public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserRequest userRequest) {
    return ApiResponse.success(userService.createUser(userRequest));
  }

  @GetMapping("/{userId}")
  @Operation(
      summary = "Get user by ID",
      description = "Retrieves profile and role details of a specific user by user ID")
  public ApiResponse<UserResponse> getUserById(@PathVariable String userId) {
    return ApiResponse.success(userService.getUserById(userId));
  }

  @DeleteMapping("/{userId}")
  @Operation(
      summary = "Delete user",
      description = "Deletes a user account from the system by user ID")
  public ApiResponse<Void> deleteUser(@PathVariable String userId) {
    userService.deleteUser(userId);
    return ApiResponse.success();
  }
}
