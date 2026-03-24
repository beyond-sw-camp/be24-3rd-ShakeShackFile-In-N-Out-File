package com.example.WaffleBear.workspace.model.relation;

import com.example.WaffleBear.user.model.User;
import com.example.WaffleBear.workspace.model.post.Post;
import io.swagger.v3.oas.annotations.media.Schema;

public class UserPostDto {

    @Schema(description = "사용자-워크스페이스 관계 생성 요청")
    public record ReqUserPost(
            @Schema(description = "사용자")
            User user,
            @Schema(description = "워크스페이스")
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

    @Schema(description = "참여자 권한 응답")
    public record ResRole(
            @Schema(description = "사용자 IDX", example = "1")
            Long idx,
            @Schema(description = "사용자 이름", example = "홍길동")
            String username,
            @Schema(description = "프로필 이미지 URL")
            String image,
            @Schema(description = "워크스페이스 내 역할", example = "EDITOR")
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
