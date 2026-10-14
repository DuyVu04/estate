package com.project.estate.modules.identity.service;

import com.project.estate.modules.identity.dto.request.PermissionRequest;
import com.project.estate.modules.identity.dto.response.PermissionResponse;
import com.project.estate.modules.identity.entity.Permission;
import com.project.estate.modules.identity.mapper.PermissionMapper;
import com.project.estate.modules.identity.repository.PermissionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PermissionService {
  private final PermissionRepository permissionRepository;

  private PermissionMapper permissionMapper;

  public PermissionResponse create(PermissionRequest permissionRequest) {
    Permission permission = permissionMapper.toPermission(permissionRequest);
    permission = permissionRepository.save(permission);
    return permissionMapper.toPermissionResponse(permission);
  }

  public List<PermissionResponse> getAll() {
    var permissions = permissionRepository.findAll();
    return permissions.stream().map(permissionMapper::toPermissionResponse).toList();
  }

  public void delete(String permission) {
    permissionRepository.deleteById(permission);
  }
}
