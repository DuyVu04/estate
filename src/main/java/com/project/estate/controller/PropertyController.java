package com.project.estate.controller;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.common.response.PageResponse;
import com.project.estate.dto.request.PropertyCreateRequest;
import com.project.estate.dto.request.PropertyUpdateRequest;
import com.project.estate.dto.response.PropertyResponse;
import com.project.estate.entity.Property;
import com.project.estate.service.PropertyService;
import com.turkraft.springfilter.boot.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/properties")
@RequiredArgsConstructor
@Tag(
    name = "Property Management",
    description =
        "Endpoints for listing, filtering, creating, updating, approving, and managing real estate properties")
public class PropertyController {

  private final PropertyService propertyService;

  @GetMapping("/{id}")
  @Operation(
      summary = "Get property by ID",
      description =
          "Retrieves complete property details including images, price, area, and location")
  public ApiResponse<PropertyResponse> getPropertyById(@PathVariable String id) {
    return ApiResponse.success(propertyService.getPropertyById(id));
  }

  @PostMapping()
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Create property",
      description =
          "Admin endpoint to create a new property listing and trigger async AI vector embedding")
  public ApiResponse<PropertyResponse> createProperty(
      @RequestBody @Valid PropertyCreateRequest request) {
    return ApiResponse.success(propertyService.createProperty(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Update property",
      description =
          "Admin endpoint to update property fields, sync image gallery, and refresh AI vector embedding")
  public ApiResponse<PropertyResponse> updateProperty(
      @PathVariable String id, @RequestBody @Valid PropertyUpdateRequest request) {
    return ApiResponse.success(propertyService.updateProperty(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Delete property",
      description = "Admin endpoint to delete a property from the database")
  public ApiResponse<Void> deleteProperty(@PathVariable String id) {
    propertyService.deleteProperty(id);
    return ApiResponse.success();
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Update property status",
      description =
          "Admin endpoint to update the status of a property (AVAILABLE, RESERVED, SOLD, HIDDEN, etc.)")
  public ApiResponse<PropertyResponse> updatePropertyStatus(
      @PathVariable String id,
      @RequestBody @Valid com.project.estate.dto.request.PropertyStatusUpdateRequest request) {
    return ApiResponse.success(propertyService.updateStatus(id, request.status()));
  }

  @PatchMapping("/{id}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Approve property listing",
      description = "Admin endpoint to approve property listing (sets status to AVAILABLE)")
  public ApiResponse<PropertyResponse> approveProperty(@PathVariable String id) {
    return ApiResponse.success(
        propertyService.updateStatus(id, com.project.estate.enums.PropertyStatus.AVAILABLE));
  }

  @PatchMapping("/{id}/reject")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Reject property listing",
      description = "Admin endpoint to reject a property listing (sets status to REJECTED)")
  public ApiResponse<PropertyResponse> rejectProperty(@PathVariable String id) {
    return ApiResponse.success(
        propertyService.updateStatus(id, com.project.estate.enums.PropertyStatus.REJECTED));
  }

  @PatchMapping("/{id}/hide")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Hide property listing",
      description =
          "Admin endpoint to temporarily hide a property from public view (sets status to HIDDEN)")
  public ApiResponse<PropertyResponse> hideProperty(@PathVariable String id) {
    return ApiResponse.success(
        propertyService.updateStatus(id, com.project.estate.enums.PropertyStatus.HIDDEN));
  }

  @GetMapping()
  @Operation(
      summary = "Filter and list properties",
      description =
          "Public endpoint to search and filter properties dynamically using SpringFilter criteria and pagination")
  public ApiResponse<PageResponse<PropertyResponse>> getPropertiesByFilter(
      @Filter Specification<Property> specification, Pageable pageable) {
    return ApiResponse.success(PageResponse.of(propertyService.search(specification, pageable)));
  }
}
