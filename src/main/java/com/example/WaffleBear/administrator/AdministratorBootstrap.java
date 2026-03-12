package com.example.WaffleBear.administrator;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserAccountStatus;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdministratorBootstrap implements ApplicationRunner {

    private static final String ADMIN_LOGIN_ID = "administrator@administrator.adm";
    private static final String ADMIN_EMAIL = "administrator@administrator.adm";
    private static final String ADMIN_NAME = "administrator";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String ADMIN_PASSWORD = "fweiuhfge2232n12@#xSD23@";

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

        if (admin.getPassword() == null || !passwordEncoder.matches(ADMIN_PASSWORD, admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        }

        userRepository.save(admin);
    }
}
