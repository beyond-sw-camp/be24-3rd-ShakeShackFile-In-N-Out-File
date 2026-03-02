package com.example.WaffleBear.user.model;

import com.example.WaffleBear.file.model.FileInfo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;

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

    @ColumnDefault(value = "'ROLE_USER'")
    private String role;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    List<FileInfo> fileInfoList;
}
