package com.example.WaffleBear.config.oauth2;


import com.example.WaffleBear.utils.Aes256;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;


@Component
public class OAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("OAUTH2_REQUEST")) {
                OAuth2AuthorizationRequest oAuth2AuthorizationRequest =
                        (OAuth2AuthorizationRequest) SerializationUtils.deserialize(cookie.getValue().getBytes());
                return oAuth2AuthorizationRequest;
            }
        }
        return null;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("OAUTH2_REQUEST",
                Aes256.encrypt(SerializationUtils.serialize(authorizationRequest)));
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(((int) Duration.ofSeconds(300L).toSeconds()));
        response.addCookie(cookie);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("OAUTH2_REQUEST")) {
                OAuth2AuthorizationRequest oAuth2AuthorizationRequest =
                        (OAuth2AuthorizationRequest) SerializationUtils.deserialize(
                                Aes256.decrypt(cookie.getValue().getBytes(StandardCharsets.UTF_8))
                        );

                cookie.setValue("");
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setSecure(true);
                cookie.setMaxAge(((int) Duration.ofSeconds(0L).toSeconds()));
                response.addCookie(cookie);

                return oAuth2AuthorizationRequest;
            }
        }
        return null;
    }
}
