package com.project.estate.repository;

import com.project.estate.entity.Property;
import com.project.estate.enums.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository
    extends JpaRepository<Property, String>, JpaSpecificationExecutor<Property> {

  long countByStatus(PropertyStatus status);
}
