package com.project.estate.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.dto.request.RoleRequest;
import com.project.estate.dto.response.RoleResponse;
import com.project.estate.service.RoleService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE ,makeFinal = true)
public class RoleController {

    RoleService roleService;
    
    @GetMapping
    ApiResponse<List<RoleResponse>> getAll() {
        return ApiResponse.success(roleService.getAll());
    }
    
    @PostMapping
    ApiResponse<RoleResponse> create(@Valid @RequestBody RoleRequest roleRequest) {
        return ApiResponse.success(roleService.create(roleRequest));
    }
    
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ApiResponse.success();
    }
    
}
