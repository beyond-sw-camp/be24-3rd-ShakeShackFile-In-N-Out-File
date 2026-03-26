package com.example.WaffleBear.file.manage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class FileManageDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolderReq {
        private String folderName;
        private Long parentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RenameReq {
        private String fileName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoveReq {
        private Long targetParentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MoveBatchReq {
        private List<Long> fileIdxList;
        private Long targetParentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestoreBatchReq {
        private List<Long> fileIdxList;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListPageReq {
        private Long parentId;
        private Integer page;
        private Integer size;
        private String sortOption;
        private String searchQuery;
        private String extensionFilter;
        private String sizeFilter;
        private String customMinSize;
        private String customMaxSize;
        private String statusFilter;
    }
}
