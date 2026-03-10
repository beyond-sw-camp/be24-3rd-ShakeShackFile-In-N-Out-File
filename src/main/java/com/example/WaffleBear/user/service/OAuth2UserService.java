package com.example.WaffleBear.user.service;

import com.example.WaffleBear.user.repository.UserRepository;
import com.example.WaffleBear.user.model.AuthUserDetails;
import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.user.model.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        UserDto.OAuth dto = UserDto.OAuth.from(oAuth2User.getAttributes(), provider);

        Optional<User> result = userRepository.findByEmail(dto.email());

        if (!result.isPresent()) {
            User user = userRepository.save(dto.toEntity());
            return AuthUserDetails.from(user);
        } else {
            User user = result.get();
            return AuthUserDetails.from(user);
        }
    }
}
