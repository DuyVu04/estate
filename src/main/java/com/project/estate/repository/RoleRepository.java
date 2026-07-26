package com.project.estate.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.estate.entity.Role;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    List<Role> findByNameIn(List<String> names);
    Optional<Role> findByName(String name);
}
