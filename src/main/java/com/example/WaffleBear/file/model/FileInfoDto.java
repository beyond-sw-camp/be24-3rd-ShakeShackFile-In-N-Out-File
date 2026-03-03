package com.example.WaffleBear.file.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class FileInfoDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileReq {
        @Schema(description = "원본 파일 이름")
        private String fileOriginName;
        private String fileFormat;
        private Long fileSize;
        // 미니오 서버에서 정보를 받을 때 사용
        private String contentType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileRes {
//        private Long fileIdx;
        private String fileOriginName;
        private String fileSaveName;
        private String fileFormat;
        private String presignedUploadUrl;
//        private String objectUrl;
        private Integer presignedUrlExpiresIn;
    }

}
