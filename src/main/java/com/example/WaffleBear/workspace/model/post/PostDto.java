package com.example.WaffleBear.workspace.model.post;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.Acceleration;

import java.time.LocalDateTime;
import java.util.List;

public class PostDto {

    @Setter
    @Getter
    public static class ReqPost {
        private Long idx;
        private String title;
        private String contents;
        @Setter
        private List<UserPost> user;

        public Post toEntity() {
            return Post.builder()
                    .idx(this.idx)
                    .title(this.title)
                    .contents(this.contents)
                    .build();
        }
    }
    @Getter
    @Builder
    public static class ResPost {
        private Long idx;
        private String title;
        private String contents;
        private boolean type;

        public static ResPost from(Post entity) {
            return ResPost.builder()
                    .idx(entity.getIdx())
                    .title(entity.getTitle())
                    .contents(entity.getContents())
                    .type(entity.getType())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ResList {
        private Long post_idx;
        private String title;
        private LocalDateTime updatedAt;
        private isShare status;
        private String UUID;

        public static ResList from(Post entity) {
            return ResList.builder()
                    .post_idx(entity.getIdx())
                    .title(entity.getTitle())
                    .updatedAt(entity.getUpdatedAt())
                    .status(entity.getStatus())
                    .UUID(entity.getUUID())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ReqType {
        private Boolean type;
        private isShare status;
    }
}
