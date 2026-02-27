package com.example.WaffleBear.posts.model;

import com.example.WaffleBear.user.model.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class PostDto {

    @Getter
    public static class ReqPost {
        private String title;
        private String contents;
        @Setter
        private User user;

        public Posts toEntity() {
            return Posts.builder()
                    .title(this.title)
                    .contents(this.contents)
                    .user_idx(user.getIdx())
                    .build();
        }
    }
    @Getter
    @Builder
    public static class ResPost {
        private String title;
        private String contents;

        public static ResPost from(Posts entity) {
            return ResPost.builder()
                    .title(entity.getTitle())
                    .contents(entity.getTitle())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ResList {
        private String title;
        private LocalDateTime updatedAt;

        public static ResList from(Posts entity) {
            return ResList.builder()
                    .title(entity.getTitle())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }
}
