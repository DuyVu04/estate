package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.PermissionRequest;
import com.project.estate.dto.response.PermissionResponse;
import com.project.estate.service.PermissionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionController {

  PermissionService permissionService;

  @GetMapping
  ApiResponse<List<PermissionResponse>> getAll() {
    return ApiResponse.success(permissionService.getAll());
  }

  @PostMapping
  ApiResponse<PermissionResponse> create(@Valid @RequestBody PermissionRequest permissionRequest) {
    return ApiResponse.success(permissionService.create(permissionRequest));
  }

  @DeleteMapping("/{id}")
  ApiResponse<Void> delete(@PathVariable String id) {
    permissionService.delete(id);
    return ApiResponse.success();
  }
}
