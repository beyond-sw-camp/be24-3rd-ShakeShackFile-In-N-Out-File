package com.example.WaffleBear.workspace.model.relation;


import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Post;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
@Table(indexes = {
        @Index(name = "idx_user_post_user_workspace", columnList = "user_id,workspace_id"),
        @Index(name = "idx_user_post_workspace_user", columnList = "workspace_id,user_id")
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
    private Post workspace;

    @Setter
    @Enumerated(EnumType.STRING)
    private AccessRole Level;


    public void updateLevel(AccessRole role) {
        this.Level = role;
    }
}
