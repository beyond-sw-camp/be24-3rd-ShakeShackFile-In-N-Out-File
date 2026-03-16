package com.example.WaffleBear.workspace.model.relation;


import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Post;
import lombok.Builder;
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
    @Getter
    @Builder
    public static class ResRole {
        private Long idx;
        private String username;
        private String Image;
        private AccessRole role;

        // Image 불러오는 로직 필요함.
        public static ResRole from(UserPost entity) {
            return ResRole.builder()
                    .idx(entity.getUser().getIdx())
                    .username(entity.getUser().getName())
                    .role(entity.getLevel())
                    .build();
        }
    }
}
