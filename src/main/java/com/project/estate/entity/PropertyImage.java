package com.project.estate.entity;

import jakarta.persistence.*;
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
@Table(name = "property_images")
public class PropertyImage extends AbstractAuditEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "property_id", nullable = false)
  Property property;

  @Column(nullable = false, length = 500)
  String url;

  @Column(name = "sort_order")
  Integer sortOrder;
}
