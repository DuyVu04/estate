package com.project.estate.modules.identity.mapper;

import com.project.estate.modules.identity.dto.request.PermissionRequest;
import com.project.estate.modules.identity.dto.response.PermissionResponse;
import com.project.estate.modules.identity.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
  Permission toPermission(PermissionRequest permissionRequest);

  PermissionResponse toPermissionResponse(Permission permission);
}
