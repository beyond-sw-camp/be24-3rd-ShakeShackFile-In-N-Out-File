package com.example.WaffleBear.workspace.model.post;

import com.example.WaffleBear.workspace.model.relation.UserPost;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @OneToMany(mappedBy = "workspace", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<UserPost> userPosts;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String contents;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean type;

    @Column(nullable = false)
    private isShare status;

    @Setter
    @Column(nullable = false)
    private String UUID;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
        this.type = false;
        this.status = isShare.Private;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String title, String contents) {
        this.title = title;
        this.contents = contents;
    }

    public void typeUpdate(Boolean type, isShare status) {
        this.type = type;
        this.status = status;
    }
}
