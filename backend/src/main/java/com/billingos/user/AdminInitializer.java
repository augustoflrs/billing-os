package com.billingos.user;

import com.billingos.common.UlidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Creates the default admin user on first boot if it doesn't exist.
 * Password is set via ADMIN_PASSWORD env var (default: admin123).
 * Change this immediately after first login in production.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        AppUser admin = userRepository.findByUsernameAndActiveTrue(adminUsername)
                .orElseGet(() -> {
                    AppUser u = new AppUser();
                    u.setId(UlidGenerator.generate());
                    u.setUsername(adminUsername);
                    u.setActive(true);
                    u.setCreatedAt(OffsetDateTime.now());
                    return u;
                });

        // Always encode and save — supports password reset via ADMIN_PASSWORD env var.
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        userRepository.save(admin);
        log.info("Admin user '{}' initialized.", adminUsername);
    }
}
