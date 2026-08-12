package com.project.estate.config;

import com.project.estate.entity.Role;
import com.project.estate.entity.User;
import com.project.estate.enums.ErrorCode;
import com.project.estate.enums.UserStatus;
import com.project.estate.exception.AppException;
import com.project.estate.repository.RoleRepository;
import com.project.estate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${default-admin:admin}")
    private String defaultAdmin;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        createAdminUserIfNotExists();
    }

    private void initRoles() {
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Administrator").build());
            log.info("✓ Created default ROLE_ADMIN");
        }
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_USER").description("Standard User").build());
            log.info("✓ Created default ROLE_USER");
        }
    }

    private void createAdminUserIfNotExists() {
        var existingAdmin = userRepository.findByUsername(defaultAdmin);
        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            admin.setPassword(passwordEncoder.encode(defaultAdmin));
            admin.setEnabled(true);
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
            log.info("✓ Default admin user verified & password synchronized! (Username: {}, Password: {})", defaultAdmin, defaultAdmin);
            return;
        }

        log.info("Creating default admin user...");

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);

        User adminUser = User.builder()
                .username(defaultAdmin)
                .firstName("System")
                .lastName("Administrator")
                .email("admin@estate.com")
                .password(passwordEncoder.encode(defaultAdmin))
                .phone("0000000000")
                .address("System")
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .roles(roles)
                .build();

        userRepository.save(adminUser);

        log.info("✓ Default admin user created successfully!");
    }
}
