package com.example.WaffleBear.file.info;

import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileInfoController {

    private final FileInfoService fileInfoService;

    @GetMapping("/{fileIdx}/properties")
    public ResponseEntity<FileInfoDto.FolderPropertyRes> getFolderProperties(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileInfoService.getFolderProperties(userIdx, fileIdx));
    }

    @GetMapping("/storage/summary")
    public ResponseEntity<FileInfoDto.StorageSummaryRes> getStorageSummary(
            @AuthenticationPrincipal AuthUserDetails dto
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileInfoService.getStorageSummary(userIdx));
    }

    @GetMapping("/{fileIdx}/text-preview")
    public ResponseEntity<FileInfoDto.TextPreviewRes> getTextPreview(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileInfoService.getTextPreview(userIdx, fileIdx));
    }
}
