package com.example.WaffleBear.posts.model;

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

    private Long user_idx;

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

    public void update()
}
