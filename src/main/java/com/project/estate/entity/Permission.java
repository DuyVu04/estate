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
@Table(name = "permissions")
public class Permission extends AbstractAuditEntity {

    @Column(nullable = false, unique = true)
    String name;

    String description;

}
