package com.example.WaffleBear.user.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.email.EmailVerify;
import com.example.WaffleBear.email.EmailVerifyRepository;
import com.example.WaffleBear.email.EmailVerifyService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserDto;
import com.example.WaffleBear.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerifyRepository emailVerifyRepository;
    private final EmailVerifyService emailVerifyService;


    public UserDto.SignupRes signup(UserDto.SignupReq dto) {
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_DUPLICATE_EMAIL);
        }

        User user = dto.toEntity();
        user.setPassword(passwordEncoder.encode(dto.password()));

        String token = UUID.randomUUID().toString();
        emailVerifyRepository.save(new EmailVerify(token, user.getEmail()));

        userRepository.save(user);
        emailVerifyService.sendVerificationEmail(user.getEmail(), token);

        return UserDto.SignupRes.from(user);
    }

    public void verifyEmail(String token) {
        EmailVerify verificationToken = emailVerifyRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("invalid verify token"));

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("expired verify token");
        }

        User user = userRepository.findByEmail(verificationToken.getEmail())
                .orElseThrow(() -> new RuntimeException("user not found"));

        user.setEnable(true);
        emailVerifyRepository.delete(verificationToken);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));

        return AuthUserDetails.from(user);
    }
}
