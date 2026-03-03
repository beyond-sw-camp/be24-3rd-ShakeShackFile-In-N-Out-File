package com.example.WaffleBear.user;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.EmailVerify;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserDto;
import jakarta.transaction.Transactional;
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

        // IDX 증가 때문에 추가함
        if(userRepository.findByEmail(dto.email()).isPresent()) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_DUPLICATE_EMAIL);
        }

        if(userRepository.findByName(dto.name()).isPresent()) {
            throw BaseException.from(BaseResponseStatus.SIGNUP_DUPLICATE_NAME);
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
                .orElseThrow(() -> new RuntimeException("유효하지 않은 토큰입니다."));

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("토큰이 만료되었습니다.");
        }

        // 2. 사용자 활성화
        User user = userRepository.findByEmail(verificationToken.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setEnable(true); // User 엔티티에 @Setter 또는 활성화 메서드 필요

        // 3. 인증 완료된 토큰 삭제 (재사용 방지)
        emailVerifyRepository.delete(verificationToken);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username).orElseThrow();

        return AuthUserDetails.from(user);
    }
}
