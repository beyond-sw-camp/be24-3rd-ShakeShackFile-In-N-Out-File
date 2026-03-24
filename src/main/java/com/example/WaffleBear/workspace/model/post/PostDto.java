package com.example.WaffleBear.workspace.model.post;

import com.example.WaffleBear.workspace.model.relation.AccessRole;
import com.example.WaffleBear.workspace.model.relation.UserPost;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class PostDto {

    @Schema(description = "워크스페이스 저장/수정 요청")
    public record ReqPost(
            @Schema(description = "워크스페이스 ID (수정 시 필수, 생성 시 null)", example = "1")
            Long idx,
            @Schema(description = "워크스페이스 제목", example = "프로젝트 기획서")
            String title,
            @Schema(description = "워크스페이스 내용 (EditorJS JSON)")
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

    @Schema(description = "워크스페이스 단건 응답")
    public record ResPost(
            @Schema(description = "워크스페이스 ID", example = "1")
            Long idx,
            @Schema(description = "제목", example = "프로젝트 기획서")
            String title,
            @Schema(description = "내용 (EditorJS JSON)")
            String contents,
            @Schema(description = "공개 여부", example = "true")
            boolean type,
            @Schema(description = "공유 상태")
            isShare status,
            @Schema(description = "UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            String uuid,
            @Schema(description = "현재 사용자의 접근 권한", example = "EDITOR")
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

    @Schema(description = "워크스페이스 목록 항목")
    public record ResList(
            @Schema(description = "워크스페이스 ID", example = "1")
            Long post_idx,
            @Schema(description = "제목", example = "프로젝트 기획서")
            String title,
            @Schema(description = "최종 수정 일시")
            LocalDateTime updatedAt,
            @Schema(description = "공유 상태")
            isShare status,
            @Schema(description = "UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            String uuid,
            @Schema(description = "참여자 권한 레벨", example = "ADMIN")
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

    @Schema(description = "UUID 조회 응답")
    public record ResUuidLookup(
            @Schema(description = "워크스페이스 ID", example = "1")
            Long idx,
            @Schema(description = "제목", example = "프로젝트 기획서")
            String title,
            @Schema(description = "UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            String uuid,
            @Schema(description = "공유 상태")
            isShare status,
            @Schema(description = "현재 사용자의 접근 권한", example = "VIEWER")
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

    @Schema(description = "공유 상태 변경 요청")
    public record ReqType(
            @Schema(description = "공개 여부", example = "true")
            Boolean type,
            @Schema(description = "공유 상태")
            isShare status
    ) {
    }
}
