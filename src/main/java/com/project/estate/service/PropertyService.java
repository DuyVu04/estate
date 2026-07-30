package com.project.estate.service;

import com.project.estate.common.response.PageResponse;
import com.project.estate.dto.request.PropertyCreateRequest;
import com.project.estate.dto.request.PropertyUpdateRequest;
import com.project.estate.dto.response.PropertyResponse;
import com.project.estate.entity.Property;
import com.project.estate.enums.ErrorCode;
import com.project.estate.enums.PropertyStatus;
import com.project.estate.exception.AppException;
import com.project.estate.mapper.PropertyMapper;
import com.project.estate.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;

    /**
     * Create a new property
     * Sets default status to AVAILABLE
     */
    @Transactional
    public PropertyResponse createProperty(PropertyCreateRequest request) {

        Property property = propertyMapper.toProperty(request);
        property.setStatus(PropertyStatus.AVAILABLE);

        propertyRepository.save(property);
        return propertyMapper.toPropertyResponse(property);
    }

    /**
     * Get property by ID
     */
    @Transactional(readOnly = true)
    public PropertyResponse getPropertyById(String id) {
        log.info("Fetching property with ID: {}", id);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        return propertyMapper.toPropertyResponse(property);
    }

    /**
     * Update property
     * Does not modify status or version
     */
    @Transactional
    public PropertyResponse updateProperty(String id, PropertyUpdateRequest request) {
        log.info("Updating property with ID: {}", id);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        propertyMapper.updateProperty(request, property);

        Property updatedProperty = propertyRepository.save(property);
        log.info("Property updated successfully with ID: {}", updatedProperty.getId());

        return propertyMapper.toPropertyResponse(updatedProperty);
    }

    /**
     * Delete property
     */
    @Transactional
    public void deleteProperty(String id) {
        log.info("Deleting property with ID: {}", id);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PROPERTY_NOT_FOUND));

        propertyRepository.delete(property);
        log.info("Property deleted successfully with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<PropertyResponse> search(
            Specification<Property> specification,
            Pageable pageable
    ) {

        return propertyRepository
                .findAll(specification, pageable)
                .map(propertyMapper::toPropertyResponse);
    }

}
