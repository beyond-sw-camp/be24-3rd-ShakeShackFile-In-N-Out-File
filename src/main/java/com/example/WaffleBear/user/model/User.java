package com.example.WaffleBear.user.model;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(unique = true)
    private String email;
    private String name;

    @Setter
    private String password;

    @Setter
    private Boolean enable;

    @Builder.Default
    private String role = "ROLE_USER";

}
