package com.inventory.config;

import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Uygulama ilk açıldığında hiç yönetici yoksa varsayılan bir yönetici hesabı oluşturur.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = new User();
        admin.setFirstName("Sistem");
        admin.setLastName("Yöneticisi");
        admin.setEmail(adminEmail.toLowerCase());
        admin.setPhone("0000000000");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        log.info("Varsayılan yönetici hesabı oluşturuldu: {}", adminEmail);
    }
}
