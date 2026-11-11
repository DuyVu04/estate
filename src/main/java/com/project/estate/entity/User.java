package com.project.estate.entity;

import com.project.estate.enums.UserStatus;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
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
@Table(name = "users")
public class User extends AbstractAuditEntity {
  String username;
  String firstName;
  String lastName;
  String email;
  String password;
  String phone;
  String address;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  UserStatus status;

  @Column(nullable = false)
  @Builder.Default
  boolean enabled = false;

  @ManyToMany()
  @Builder.Default
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();
}
