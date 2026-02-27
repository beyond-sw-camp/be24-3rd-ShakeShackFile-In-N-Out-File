package com.example.WaffleBear.user.model;

import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Builder
public class AuthUserDetails implements UserDetails {
    private Long idx;
    private String email;
    private String password;
    private Boolean enable;
    private String role;

    public static AuthUserDetails from(User entity) {
        return AuthUserDetails.builder()
                .idx(entity.getIdx())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .enable(entity.getEnable())
                .role(entity.getRole())
                .build();
    }

    public Long getIdx() {
        return idx;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enable;
    }
}
