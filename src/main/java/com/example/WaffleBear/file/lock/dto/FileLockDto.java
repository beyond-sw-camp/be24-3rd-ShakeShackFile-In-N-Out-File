package com.example.WaffleBear.file.lock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class FileLockDto {

    @Schema(description = "파일 잠금/해제 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LockReq {
        @Schema(description = "잠금/해제 대상 파일 ID 목록", example = "[1, 2, 3]")
        private List<Long> fileIdxList;
        @Schema(description = "잠금 여부 (true: 잠금, false: 해제)", example = "true")
        private Boolean locked;
    }
}
