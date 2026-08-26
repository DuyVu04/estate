package com.project.estate.service;

import com.project.estate.dto.request.PropertyCreateRequest;
import com.project.estate.dto.request.PropertyUpdateRequest;
import com.project.estate.dto.response.PropertyResponse;
import com.project.estate.entity.Property;
import com.project.estate.entity.PropertyImage;
import com.project.estate.enums.ErrorCode;
import com.project.estate.enums.PropertyStatus;
import com.project.estate.exception.AppException;
import com.project.estate.mapper.PropertyMapper;
import com.project.estate.messaging.producer.PropertyVectorProducer;
import com.project.estate.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final PropertyMapper propertyMapper;
  private final PropertyVectorProducer propertyVectorProducer;
  private final MinioService minioService;

  /** Create a new property Sets default status to AVAILABLE and persists images */
  @Transactional
  public PropertyResponse createProperty(PropertyCreateRequest request) {
    Property property = propertyMapper.toProperty(request);
    property.setStatus(PropertyStatus.AVAILABLE);

    if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
      for (int i = 0; i < request.imageUrls().size(); i++) {
        String rawUrl = request.imageUrls().get(i);
        if (rawUrl != null && !rawUrl.isBlank()) {
          String objectKey = minioService.extractObjectName(rawUrl);
          property.addImage(PropertyImage.builder().url(objectKey).sortOrder(i).build());
        }
      }
    }

    propertyRepository.save(property);
    propertyVectorProducer.publishPropertyEmbeddingTask(property.getId());
    return propertyMapper.toPropertyResponse(property);
  }

  /** Get property by ID - Cached in Redis (key: property_detail::{id}) */
  @Cacheable(value = "property_detail", key = "#id")
  @Transactional(readOnly = true)
  public PropertyResponse getPropertyById(String id) {
    log.info("Fetching property from DB for ID: {}", id);

    Property property =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

    return propertyMapper.toPropertyResponse(property);
  }

  /** Update property - Evict Redis Cache on modification and sync images */
  @CacheEvict(value = "property_detail", key = "#id")
  @Transactional
  public PropertyResponse updateProperty(String id, PropertyUpdateRequest request) {
    log.info("Updating property with ID: {}", id);

    Property property =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

    propertyMapper.updateProperty(request, property);

    if (request.imageUrls() != null) {
      property.getImages().clear();
      for (int i = 0; i < request.imageUrls().size(); i++) {
        String rawUrl = request.imageUrls().get(i);
        if (rawUrl != null && !rawUrl.isBlank()) {
          String objectKey = minioService.extractObjectName(rawUrl);
          property.addImage(PropertyImage.builder().url(objectKey).sortOrder(i).build());
        }
      }
    }

    Property updatedProperty = propertyRepository.save(property);
    propertyVectorProducer.publishPropertyEmbeddingTask(updatedProperty.getId());
    log.info("Property updated successfully with ID: {}", updatedProperty.getId());

    return propertyMapper.toPropertyResponse(updatedProperty);
  }

  /** Delete property - Evict Redis Cache on deletion */
  @CacheEvict(value = "property_detail", key = "#id")
  @Transactional
  public void deleteProperty(String id) {
    log.info("Deleting property with ID: {}", id);

    Property property =
        propertyRepository
            .findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

    propertyRepository.delete(property);
    log.info("Property deleted successfully with ID: {}", id);
  }

  @Transactional(readOnly = true)
  public Page<PropertyResponse> search(Specification<Property> specification, Pageable pageable) {

    return propertyRepository
        .findAll(specification, pageable)
        .map(propertyMapper::toPropertyResponse);
  }
}
