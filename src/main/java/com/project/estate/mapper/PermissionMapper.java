package com.project.estate.mapper;

import com.project.estate.dto.request.PermissionRequest;
import com.project.estate.dto.response.PermissionResponse;
import com.project.estate.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest permissionRequest);


    PermissionResponse toPermissionResponse(Permission permission);
}
