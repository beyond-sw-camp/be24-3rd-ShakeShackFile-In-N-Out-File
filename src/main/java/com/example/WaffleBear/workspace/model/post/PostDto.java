package com.example.WaffleBear.workspace.model.post;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class PostDto {

    @Setter
    @Getter
    public static class ReqPost {
        private String title;
        private String contents;
        @Setter
        private List<UserPost> user;

        public Post toEntity() {
            return Post.builder()
                    .title(this.title)
                    .contents(this.contents)
//                    .userPosts(this.user)
                    .build();
        }
    }
    @Getter
    @Builder
    public static class ResPost {
        private String title;
        private String contents;

        public static ResPost from(Post entity) {
            return ResPost.builder()
                    .title(entity.getTitle())
                    .contents(entity.getTitle())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ResList {
        private Long post_idx;
        private String title;
        private LocalDateTime updatedAt;

        public static ResList from(Post entity) {
            return ResList.builder()
                    .post_idx(entity.getIdx())
                    .title(entity.getTitle())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }
}
