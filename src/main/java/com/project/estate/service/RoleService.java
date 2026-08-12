package com.project.estate.service;

import com.project.estate.dto.request.RoleRequest;
import com.project.estate.dto.response.RoleResponse;
import com.project.estate.mapper.RoleMapper;
import com.project.estate.repository.PermissionRepository;
import com.project.estate.repository.RoleRepository;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RoleService {
  private final RoleRepository roleRepository;

  private final RoleMapper roleMapper;

  private final PermissionRepository permissionRepository;

  public RoleResponse create(RoleRequest request) {
    var role = roleMapper.toRole(request);
    var permission = permissionRepository.findAllById(request.permissions());
    role.setPermissions(new HashSet<>(permission));
    roleRepository.save(role);
    return roleMapper.toRoleResponse(role);
  }

  public List<RoleResponse> getAll() {
    var roles = roleRepository.findAll();
    return roles.stream().map(roleMapper::toRoleResponse).toList();
  }

  public void delete(String role) {
    roleRepository.deleteById(role);
  }
}
