package com.project.estate.repository;

import com.project.estate.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
  List<Role> findByNameIn(List<String> names);

  Optional<Role> findByName(String name);
}
