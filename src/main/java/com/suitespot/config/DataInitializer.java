package com.suitespot.config;

import com.suitespot.entity.User;
import com.suitespot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create or update admin user
        userRepository.findByUsername("admin").ifPresentOrElse(
            existingAdmin -> {
                // Update existing admin user to ensure correct password and role
                existingAdmin.setPassword(passwordEncoder.encode("admin123"));
                existingAdmin.setRole(User.Role.ADMIN);
                existingAdmin.setActive(true);
                existingAdmin.setEmail("admin@suitespot.com");
                if (existingAdmin.getFullName() == null || existingAdmin.getFullName().isEmpty()) {
                    existingAdmin.setFullName("Administrator");
                }
                userRepository.save(existingAdmin);
            },
            () -> {
                // Create new admin user
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .email("admin@suitespot.com")
                        .fullName("Administrator")
                        .role(User.Role.ADMIN)
                        .active(true)
                        .build();
                userRepository.save(admin);
            }
        );
    }
}

