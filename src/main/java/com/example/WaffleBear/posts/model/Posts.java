package com.example.WaffleBear.posts.model;

import com.example.WaffleBear.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
public class Posts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_idx")
    private Long idx;

    // User 엔티티의 idx를 외래키로 참조 (N:1 관계)
    @ManyToOne
    @JoinColumn(name = "user_idx")
    @Setter
    private User user;

    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contents;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    public void update(Posts post) {
        Posts.builder()
                .title(post.getTitle())
                .contents(post.getContents())
                .user(post.getUser())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
