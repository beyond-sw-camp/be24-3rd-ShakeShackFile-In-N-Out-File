package com.example.WaffleBear.workspace.model.post;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_idx")
    private Long idx;

    // User 엔티티의 idx를 외래키로 참조 (N:1 관계)
    @OneToMany(mappedBy = "post")
    private List<UserPost> userPosts;

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

    public void update(Post post) {
        Post.builder()
                .title(post.getTitle())
                .contents(post.getContents())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
