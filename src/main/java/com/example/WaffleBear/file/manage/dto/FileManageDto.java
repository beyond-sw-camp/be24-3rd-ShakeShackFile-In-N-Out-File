package com.example.WaffleBear.file.manage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class FileManageDto {

    @Schema(description = "폴더 생성 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolderReq {
        @Schema(description = "폴더명", example = "새 폴더")
        private String folderName;
        @Schema(description = "상위 폴더 ID (루트인 경우 null)", example = "5")
        private Long parentId;
    }

    @Schema(description = "파일/폴더 이름 변경 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RenameReq {
        @Schema(description = "변경할 파일/폴더명", example = "새로운이름.pdf")
        private String fileName;
    }

    @Schema(description = "파일/폴더 이동 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoveReq {
        @Schema(description = "이동 대상 폴더 ID", example = "10")
        private Long targetParentId;
    }

    @Schema(description = "파일/폴더 일괄 이동 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoveBatchReq {
        @Schema(description = "이동할 파일 ID 목록", example = "[1, 2, 3]")
        private List<Long> fileIdxList;
        @Schema(description = "이동 대상 폴더 ID", example = "10")
        private Long targetParentId;
    }

    @Schema(description = "파일/폴더 일괄 복원 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestoreBatchReq {
        @Schema(description = "복원할 파일 ID 목록", example = "[1, 2, 3]")
        private List<Long> fileIdxList;
    }
}
