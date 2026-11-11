package com.project.estate.mapper;

import com.project.estate.dto.request.UserRequest;
import com.project.estate.dto.response.UserResponse;
import com.project.estate.entity.User;
import org.mapstruct.Mapper;

@Mapper(
    componentModel = "spring",
    uses = {RoleMapper.class})
public interface UserMapper {
  UserResponse toUserResponse(User user);

  User toUser(UserRequest userRequest);
}
