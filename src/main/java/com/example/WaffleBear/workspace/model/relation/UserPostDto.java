package com.example.WaffleBear.workspace.model.relation;


import com.example.WaffleBear.user.model.User;

import com.example.WaffleBear.workspace.model.post.Workspace;
import lombok.Getter;

public class UserPostDto {

    @Getter
    public static class ReqUserPost {
        private User user;
        private Workspace post;

        public UserPost toEntity(Workspace result, User user) {
            return UserPost.builder()
                    .workspace(result)
                    .user(user)
                    .Level(AccessRole.ADMIN)
                    .build();
        }
    }
}
