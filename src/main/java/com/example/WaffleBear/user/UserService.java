package com.example.WaffleBear.user;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDto.SignupRes signup(UserDto.SignupReq dto) {
        User user = dto.toEntity();
        user.setPassword(passwordEncoder.encode(dto.password()));
        userRepository.save(user);
        return UserDto.SignupRes.from(user);
    }
}
