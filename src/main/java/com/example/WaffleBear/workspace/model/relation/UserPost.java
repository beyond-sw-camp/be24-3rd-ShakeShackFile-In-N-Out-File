package com.example.WaffleBear.workspace.model.relation;


import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Workspace;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(name = "user_post", indexes = {
        @Index(name = "idx_post_in_user", columnList = "user_id, workspace_id"),
        @Index(name = "idx_user_in_post", columnList = "workspace_id, user_id")
})
public class UserPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    private AccessRole Level;
}
