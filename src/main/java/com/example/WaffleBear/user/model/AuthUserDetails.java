package com.example.WaffleBear.user.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Builder
public class AuthUserDetails implements UserDetails, OAuth2User {
    @Getter
    private Long idx;

    @Getter
    private String id;

    @Getter
    private String email;

    private String password;
    private Boolean enable;

    @Getter
    private String role;

    private String name;
    private Map<String, Object> attributes;
    private UserAccountStatus accountStatus;

    public static AuthUserDetails from(User entity) {
        return AuthUserDetails.builder()
                .idx(entity.getIdx())
                .id(entity.getEmail())
                .email(entity.getEmail())
                .name(entity.getName())
                .password(entity.getPassword())
                .enable(entity.getEnable())
                .role(entity.getRole())
                .accountStatus(entity.getAccountStatus() == null ? UserAccountStatus.ACTIVE : entity.getAccountStatus())
                .build();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus == null || accountStatus == UserAccountStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enable) && (accountStatus == null || accountStatus == UserAccountStatus.ACTIVE);
    }
}
