package com.example.WaffleBear.workspace.model.relation;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Post;

public class UserPostDto {

    public record ReqUserPost(
            User user,
            Post post
    ) {
        public UserPost toEntity(Post result, User user) {
            return UserPost.builder()
                    .user(user)
                    .workspace(result)
                    .Level(AccessRole.ADMIN)
                    .build();
        }
    }

    public record ResRole(
            Long idx,
            String username,
            String image,
            AccessRole role
    ) {
        public static ResRole from(UserPost entity) {
            return new ResRole(
                    entity.getUser().getIdx(),
                    entity.getUser().getName(),
                    null,
                    entity.getLevel()
            );
        }
    }
}
