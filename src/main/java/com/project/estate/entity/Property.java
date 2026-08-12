package com.project.estate.entity;

import com.project.estate.enums.PropertyStatus;
import com.project.estate.enums.PropertyType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "properties")
public class Property extends AbstractAuditEntity {

  @Column(nullable = false)
  String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "property_type", nullable = false, length = 20)
  PropertyType propertyType;

  @Column(nullable = false)
  String address;

  @Column(nullable = false)
  String ward;

  @Column(nullable = false)
  String district;

  @Column(nullable = false)
  String city;

  @Column(nullable = false, precision = 10, scale = 2)
  BigDecimal area;

  @Column(nullable = false, precision = 15, scale = 2)
  BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  PropertyStatus status;

  @Version
  @Column(nullable = false)
  Long version;

  @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  List<PropertyImage> images = new ArrayList<>();

  /** Helper method to add image to property */
  public void addImage(PropertyImage image) {
    images.add(image);
    image.setProperty(this);
  }

  /** Helper method to remove image from property */
  public void removeImage(PropertyImage image) {
    images.remove(image);
    image.setProperty(null);
  }
}
