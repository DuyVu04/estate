package com.project.estate.entity;

import com.project.estate.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

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
    boolean enabled = false;

    @ManyToMany()
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

}