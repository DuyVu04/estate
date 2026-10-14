package com.project.estate.modules.identity.entity;

import com.project.estate.shared.entity.AbstractAuditEntity;
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
@Table(name = "permissions")
public class Permission extends AbstractAuditEntity {

  @Column(nullable = false, unique = true)
  String name;

  String description;
}
