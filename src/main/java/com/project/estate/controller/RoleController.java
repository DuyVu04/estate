package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.RoleRequest;
import com.project.estate.dto.response.RoleResponse;
import com.project.estate.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
    name = "Role Management",
    description = "Endpoints for managing user roles and role-permission mappings")
public class RoleController {

  RoleService roleService;

  @GetMapping
  @Operation(
      summary = "Get all roles",
      description = "Retrieves a complete list of roles along with their associated permissions")
  ApiResponse<List<RoleResponse>> getAll() {
    return ApiResponse.success(roleService.getAll());
  }

  @PostMapping
  @Operation(
      summary = "Create role",
      description = "Creates a new system role and associates permissions")
  ApiResponse<RoleResponse> create(@Valid @RequestBody RoleRequest roleRequest) {
    return ApiResponse.success(roleService.create(roleRequest));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete role", description = "Deletes a role definition by its ID")
  ApiResponse<Void> delete(@PathVariable String id) {
    roleService.delete(id);
    return ApiResponse.success();
  }
}
