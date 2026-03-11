package com.example.WaffleBear.workspace.model.relation;


import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Post;
import lombok.Getter;

public class UserPostDto {

    @Getter
    public static class ReqUserPost {
        private User user;
        private Post post;

        public UserPost toEntity(Post result, User user) {
            return UserPost.builder()
                    .user(user)
                    .workspace(result)
                    .Level(AccessRole.ADMIN)
                    .build();
        }
    }
}
