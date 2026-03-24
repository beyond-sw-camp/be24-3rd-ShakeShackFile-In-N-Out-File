package com.example.WaffleBear.file.info.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class FileInfoDto {

    @Schema(description = "폴더 속성 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolderPropertyRes {
        @Schema(description = "폴더 고유 ID", example = "1")
        private Long idx;
        @Schema(description = "폴더명", example = "문서")
        private String folderName;
        @Schema(description = "상위 폴더 ID", example = "0")
        private Long parentId;
        @Schema(description = "직접 하위 항목 수", example = "10")
        private Integer directChildCount;
        @Schema(description = "직접 하위 파일 수", example = "7")
        private Integer directFileCount;
        @Schema(description = "직접 하위 폴더 수", example = "3")
        private Integer directFolderCount;
        @Schema(description = "전체 하위 항목 수 (재귀)", example = "50")
        private Integer totalChildCount;
        @Schema(description = "전체 하위 파일 수 (재귀)", example = "40")
        private Integer totalFileCount;
        @Schema(description = "전체 하위 폴더 수 (재귀)", example = "10")
        private Integer totalFolderCount;
        @Schema(description = "직접 하위 파일 크기 합계 (바이트)", example = "1048576")
        private Long directSize;
        @Schema(description = "전체 하위 파일 크기 합계 (바이트)", example = "5242880")
        private Long totalSize;
        @Schema(description = "폴더 생성 일시")
        private LocalDateTime uploadDate;
        @Schema(description = "최종 수정 일시")
        private LocalDateTime lastModifyDate;
    }

    @Schema(description = "스토리지 카테고리별 사용 현황")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageCategoryRes {
        @Schema(description = "카테고리 코드", example = "IMAGE")
        private String categoryKey;
        @Schema(description = "카테고리 표시명", example = "이미지")
        private String categoryLabel;
        @Schema(description = "파일 개수", example = "25")
        private Integer fileCount;
        @Schema(description = "사용 용량 (바이트)", example = "52428800")
        private Long sizeBytes;
        @Schema(description = "전체 대비 사용 비율 (%)", example = "30")
        private Integer usagePercent;
    }

    @Schema(description = "용량 상위 파일 정보")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageTopFileRes {
        @Schema(description = "파일 고유 ID", example = "1")
        private Long idx;
        @Schema(description = "원본 파일명", example = "동영상.mp4")
        private String fileOriginName;
        @Schema(description = "파일 확장자", example = "mp4")
        private String fileFormat;
        @Schema(description = "파일 크기 (바이트)", example = "104857600")
        private Long fileSize;
        @Schema(description = "최종 수정 일시")
        private LocalDateTime lastModifyDate;
        @Schema(description = "상위 폴더 ID", example = "5")
        private Long parentId;
    }

    @Schema(description = "스토리지 사용 요약 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorageSummaryRes {
        @Schema(description = "요금제 코드", example = "PRO")
        private String planCode;
        @Schema(description = "요금제 표시명", example = "프로 플랜")
        private String planLabel;
        @Schema(description = "관리자 계정 여부", example = "false")
        private Boolean adminAccount;
        @Schema(description = "파일 공유 기능 사용 가능 여부", example = "true")
        private Boolean shareEnabled;
        @Schema(description = "파일 잠금 기능 사용 가능 여부", example = "true")
        private Boolean fileLockEnabled;
        @Schema(description = "최대 업로드 파일 크기 (바이트)", example = "104857600")
        private Long maxUploadFileBytes;
        @Schema(description = "최대 동시 업로드 파일 수", example = "10")
        private Integer maxUploadCount;
        @Schema(description = "전체 할당 용량 (바이트)", example = "10737418240")
        private Long quotaBytes;
        @Schema(description = "기본 할당 용량 (바이트)", example = "5368709120")
        private Long baseQuotaBytes;
        @Schema(description = "추가 구매 용량 (바이트)", example = "5368709120")
        private Long addonQuotaBytes;
        @Schema(description = "사용 중인 용량 (바이트)", example = "3221225472")
        private Long usedBytes;
        @Schema(description = "활성 파일 사용 용량 (바이트)", example = "2684354560")
        private Long activeUsedBytes;
        @Schema(description = "휴지통 사용 용량 (바이트)", example = "536870912")
        private Long trashUsedBytes;
        @Schema(description = "남은 용량 (바이트)", example = "7516192768")
        private Long remainingBytes;
        @Schema(description = "용량 사용률 (%)", example = "30")
        private Integer usagePercent;
        @Schema(description = "전체 파일 수", example = "150")
        private Integer totalFileCount;
        @Schema(description = "활성 파일 수", example = "120")
        private Integer activeFileCount;
        @Schema(description = "휴지통 파일 수", example = "30")
        private Integer trashFileCount;
        @Schema(description = "활성 폴더 수", example = "20")
        private Integer activeFolderCount;
        @Schema(description = "휴지통 폴더 수", example = "5")
        private Integer trashFolderCount;
        @Schema(description = "카테고리별 사용 현황 목록")
        private List<StorageCategoryRes> categories;
        @Schema(description = "용량 상위 파일 목록")
        private List<StorageTopFileRes> largestFiles;
    }

    @Schema(description = "텍스트 파일 미리보기 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TextPreviewRes {
        @Schema(description = "파일 고유 ID", example = "1")
        private Long idx;
        @Schema(description = "원본 파일명", example = "readme.txt")
        private String fileOriginName;
        @Schema(description = "파일 확장자", example = "txt")
        private String fileFormat;
        @Schema(description = "Content-Type", example = "text/plain")
        private String contentType;
        @Schema(description = "파일 텍스트 내용")
        private String content;
        @Schema(description = "내용 잘림 여부", example = "false")
        private Boolean truncated;
        @Schema(description = "파일 크기 (바이트)", example = "2048")
        private Long fileSize;
    }
}
