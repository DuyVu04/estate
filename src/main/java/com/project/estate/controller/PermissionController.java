package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.PermissionRequest;
import com.project.estate.dto.response.PermissionResponse;
import com.project.estate.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Permission Management", description = "Endpoints for managing system RBAC permissions")
public class PermissionController {

  PermissionService permissionService;

  @GetMapping
  @Operation(
      summary = "Get all permissions",
      description = "Retrieves a complete list of defined permissions in the system")
  ApiResponse<List<PermissionResponse>> getAll() {
    return ApiResponse.success(permissionService.getAll());
  }

  @PostMapping
  @Operation(
      summary = "Create permission",
      description = "Creates a new system permission with name and description")
  ApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionRequest permissionRequest) {
    return ApiResponse.success(permissionService.create(permissionRequest));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete permission",
      description = "Deletes a permission definition by its ID")
  ApiResponse<Void> delete(@PathVariable String id) {
    permissionService.delete(id);
    return ApiResponse.success();
  }
}
