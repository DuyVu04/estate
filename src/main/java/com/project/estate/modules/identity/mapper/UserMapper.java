package com.project.estate.modules.identity.mapper;

import com.project.estate.modules.identity.dto.request.UserRequest;
import com.project.estate.modules.identity.dto.response.UserResponse;
import com.project.estate.modules.identity.entity.User;
import org.mapstruct.Mapper;

@Mapper(
    componentModel = "spring",
    uses = {RoleMapper.class})
public interface UserMapper {
  UserResponse toUserResponse(User user);

  User toUser(UserRequest userRequest);
}
