package com.project.estate.config;

import com.project.estate.entity.Role;
import com.project.estate.entity.User;
import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import com.project.estate.repository.RoleRepository;
import com.project.estate.repository.UserRepository;
import com.project.estate.enums.UserStatus;
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

    @Value("${default-admin}")
    private String defaultAdmin;


    @Override
    @Transactional
    public void run(String... args) {
        createAdminUserIfNotExists();
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

        // Find ROLE_ADMIN
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // Create admin user
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
        log.info("  Username: admin");
        log.info("  Password: admin");
        log.info("  ⚠️  Please change the password in production!");
    }
}
