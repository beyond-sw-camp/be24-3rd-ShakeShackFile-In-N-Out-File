package com.example.WaffleBear.workspace.model.post;

import com.example.WaffleBear.workspace.model.relation.AccessRole;
import com.example.WaffleBear.workspace.model.relation.UserPost;

import java.time.LocalDateTime;

public class PostDto {

    public record ReqPost(
            Long idx,
            String title,
            String contents
    ) {
        public Post toEntity() {
            return Post.builder()
                    .idx(idx)
                    .title(title)
                    .contents(contents)
                    .build();
        }
    }

    public record ResPost(
            Long idx,
            String title,
            String contents,
            boolean type,
            isShare status,
            String uuid,
            AccessRole accessRole
    ) {
        public static ResPost from(Post entity, AccessRole accessRole) {
            return new ResPost(
                    entity.getIdx(),
                    entity.getTitle(),
                    entity.getContents(),
                    entity.getType(),
                    entity.getStatus(),
                    entity.getUUID(),
                    accessRole
            );
        }
    }

    public record ResList(
            Long post_idx,
            String title,
            LocalDateTime updatedAt,
            isShare status,
            String uuid,
            AccessRole level
    ) {
        public static ResList from(UserPost relation) {
            Post workspace = relation.getWorkspace();
            return new ResList(
                    workspace.getIdx(),
                    workspace.getTitle(),
                    workspace.getUpdatedAt(),
                    workspace.getStatus(),
                    workspace.getUUID(),
                    relation.getLevel()
            );
        }
    }

    public record ResUuidLookup(
            Long idx,
            String title,
            String uuid,
            isShare status,
            AccessRole accessRole
    ) {
        public static ResUuidLookup from(Post entity, AccessRole accessRole) {
            return new ResUuidLookup(
                    entity.getIdx(),
                    entity.getTitle(),
                    entity.getUUID(),
                    entity.getStatus(),
                    accessRole
            );
        }
    }

    public record ReqType(
            Boolean type,
            isShare status
    ) {
    }
}
