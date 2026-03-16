package com.example.WaffleBear.administrator;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserAccountStatus;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdministratorBootstrap implements ApplicationRunner {

    @Value("${admin.email}") private String ADMIN_EMAIL;
    @Value("${admin.name}") private String ADMIN_NAME;
    @Value("${admin.role}") private String ADMIN_ROLE;
    @Value("${admin.password}") private String ADMIN_PASSWORD;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        User admin = userRepository.findByEmail(ADMIN_EMAIL)
                .orElseGet(() -> User.builder()
                        .email(ADMIN_EMAIL)
                        .name(ADMIN_NAME)
                        .enable(true)
                        .role(ADMIN_ROLE)
                        .accountStatus(UserAccountStatus.ACTIVE)
                        .build());

        admin.setEmail(ADMIN_EMAIL);
        admin.setName(ADMIN_NAME);
        admin.setEnable(true);
        admin.setRole(ADMIN_ROLE);
        admin.setAccountStatus(UserAccountStatus.ACTIVE);

        if (shouldResetPassword(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        }

        userRepository.save(admin);
    }

    private boolean shouldResetPassword(String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return true;
        }

        try {
            return !passwordEncoder.matches(ADMIN_PASSWORD, storedPassword);
        } catch (IllegalArgumentException exception) {
            // Legacy plain-text or unknown encoded passwords should be replaced
            // with the configured administrator password on startup.
            return true;
        }
    }
}
