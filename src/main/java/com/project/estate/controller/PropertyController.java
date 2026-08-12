package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.common.response.PageResponse;
import com.project.estate.dto.request.PropertyCreateRequest;
import com.project.estate.dto.request.PropertyUpdateRequest;
import com.project.estate.dto.response.PropertyResponse;
import com.project.estate.entity.Property;
import com.project.estate.service.PropertyService;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

  private final PropertyService propertyService;

  /** Public endpoint - Get property by ID */
  @GetMapping("/{id}")
  public ApiResponse<PropertyResponse> getPropertyById(@PathVariable String id) {
    return ApiResponse.success(propertyService.getPropertyById(id));
  }

  /** Admin endpoint - Create property */
  @PostMapping()
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<PropertyResponse> createProperty(
      @RequestBody @Valid PropertyCreateRequest request) {
    return ApiResponse.success(propertyService.createProperty(request));
  }

  /** Admin endpoint - Update property */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<PropertyResponse> updateProperty(
      @PathVariable String id, @RequestBody @Valid PropertyUpdateRequest request) {
    return ApiResponse.success(propertyService.updateProperty(id, request));
  }

  /** Admin endpoint - Delete property */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> deleteProperty(@PathVariable String id) {
    propertyService.deleteProperty(id);
    return ApiResponse.success();
  }

  @GetMapping()
  public ApiResponse<PageResponse<PropertyResponse>> getPropertiesByFilter(
      @Filter Specification<Property> specification, Pageable pageable) {
    return ApiResponse.success(PageResponse.of(propertyService.search(specification, pageable)));
  }
}
