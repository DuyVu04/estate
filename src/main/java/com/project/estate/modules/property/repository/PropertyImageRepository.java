package com.project.estate.modules.property.repository;

import com.project.estate.modules.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, String> {}
