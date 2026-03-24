package com.example.WaffleBear.group.model.dto;

import com.example.WaffleBear.group.model.enums.InviteType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class GroupShareDto {

    @Schema(description = "파일 공유 요청 (그룹/사용자/이메일 대상)")
    public record FileShareRequest(
            @Schema(description = "공유할 파일 ID 목록", example = "[1, 2, 3]")
            List<Long> fileIds,
            @Schema(description = "공유 대상 사용자 IDX 목록", example = "[10, 20]")
            List<Long> userIds,
            @Schema(description = "공유 대상 그룹 ID 목록", example = "[5]")
            List<Long> groupIds,
            @Schema(description = "공유 대상 이메일 목록", example = "[\"user@example.com\"]")
            List<String> emails
    ) {
    }

    @Schema(description = "워크스페이스 공유 요청")
    public record WorkspaceShareRequest(
            @Schema(description = "워크스페이스 ID", example = "1")
            Long workspaceId,
            @Schema(description = "공유 대상 사용자 IDX 목록", example = "[10, 20]")
            List<Long> userIds,
            @Schema(description = "공유 대상 그룹 ID 목록", example = "[5]")
            List<Long> groupIds,
            @Schema(description = "공유 대상 이메일 목록", example = "[\"user@example.com\"]")
            List<String> emails
    ) {
    }

    @Schema(description = "채팅방 공유 요청")
    public record ChatShareRequest(
            @Schema(description = "채팅방 ID", example = "1")
            Long roomId,
            @Schema(description = "공유 대상 사용자 IDX 목록", example = "[10, 20]")
            List<Long> userIds,
            @Schema(description = "공유 대상 그룹 ID 목록", example = "[5]")
            List<Long> groupIds,
            @Schema(description = "공유 대상 이메일 목록", example = "[\"user@example.com\"]")
            List<String> emails
    ) {
    }

    @Schema(description = "공유 대상자 정보")
    public record ShareRecipient(
            @Schema(description = "사용자 IDX", example = "10")
            Long userId,
            @Schema(description = "사용자 이름", example = "홍길동")
            String name,
            @Schema(description = "사용자 이메일", example = "user@example.com")
            String email,
            @Schema(description = "대상 소스 (USER/GROUP/EMAIL)", example = "GROUP")
            String source
    ) {
    }

    @Schema(description = "대기 중인 초대 정보")
    public record PendingInvite(
            @Schema(description = "초대 ID", example = "1")
            Long inviteId,
            @Schema(description = "초대 이메일", example = "newuser@example.com")
            String email,
            @Schema(description = "초대 유형", example = "FRIEND")
            InviteType type
    ) {
    }

    @Schema(description = "공유 결과 응답")
    public record ShareResult(
            @Schema(description = "영향받은 대상 수", example = "5")
            int affectedCount,
            @Schema(description = "확정된 공유 대상자 목록")
            List<ShareRecipient> resolvedRecipients,
            @Schema(description = "대기 중인 초대 목록")
            List<PendingInvite> pendingInvites
    ) {
    }
}
