package com.project.estate.mapper;

import com.project.estate.dto.request.PropertyCreateRequest;
import com.project.estate.dto.request.PropertyUpdateRequest;
import com.project.estate.dto.response.PropertyResponse;
import com.project.estate.entity.Property;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PropertyMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "images", ignore = true)
  Property toProperty(PropertyCreateRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "images", ignore = true)
  void updateProperty(PropertyUpdateRequest request, @MappingTarget Property property);

  @Mapping(
      target = "imageUrls",
      expression =
          "java(property.getImages() != null ? property.getImages().stream().map(com.project.estate.entity.PropertyImage::getUrl).toList() : java.util.List.of())")
  PropertyResponse toPropertyResponse(Property property);
}
