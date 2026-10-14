package com.project.estate.modules.identity.repository;

import com.project.estate.modules.identity.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository
    extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
  @EntityGraph(attributePaths = {"roles", "roles.permissions"})
  Optional<User> findByUsername(String username);

  Page<User> findAll(Pageable pageable);

  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);
}
