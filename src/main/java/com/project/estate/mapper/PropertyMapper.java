package com.project.estate.mapper;

import com.project.estate.dto.request.PropertyCreateRequest;
import com.project.estate.dto.request.PropertyUpdateRequest;
import com.project.estate.dto.response.PropertyResponse;
import com.project.estate.entity.Property;
import com.project.estate.service.MinioService;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class PropertyMapper {

  @Autowired protected MinioService minioService;

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "images", ignore = true)
  public abstract Property toProperty(PropertyCreateRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "images", ignore = true)
  public abstract void updateProperty(
      PropertyUpdateRequest request, @MappingTarget Property property);

  @Mapping(target = "thumbnailUrl", expression = "java(mapThumbnailUrl(property))")
  @Mapping(target = "imageUrls", expression = "java(mapImagesToUrls(property))")
  public abstract PropertyResponse toPropertyResponse(Property property);

  protected String mapThumbnailUrl(Property property) {
    if (property == null || property.getImages() == null || property.getImages().isEmpty()) {
      return null;
    }
    String firstUrl = property.getImages().get(0).getUrl();
    return minioService != null ? minioService.buildFullUrl(firstUrl) : firstUrl;
  }

  protected List<String> mapImagesToUrls(Property property) {
    if (property == null || property.getImages() == null) {
      return List.of();
    }
    return property.getImages().stream()
        .map(img -> minioService != null ? minioService.buildFullUrl(img.getUrl()) : img.getUrl())
        .toList();
  }
}
