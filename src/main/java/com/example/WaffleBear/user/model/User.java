package com.example.WaffleBear.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Setter
    @Column(unique = true)
    private String email;

    @Setter
    private String name;

    @Setter
    private String password;

    @Setter
    private Boolean enable;

    @Setter
    @ColumnDefault(value = "'ROLE_USER'")
    private String role;

    @Setter
    @Enumerated(EnumType.STRING)
    private UserAccountStatus accountStatus;

    @PrePersist
    public void prePersist() {
        if (role == null || role.isBlank()) {
            role = "ROLE_USER";
        }
        if (accountStatus == null) {
            accountStatus = UserAccountStatus.ACTIVE;
        }
    }
}
