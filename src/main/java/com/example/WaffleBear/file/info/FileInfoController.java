package com.example.WaffleBear.file.info;

import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Drive Info", description = "Drive property and preview APIs")
@SecurityRequirement(name = "bearerAuth")
public class FileInfoController {

    private final FileInfoService fileInfoService;

    @GetMapping("/{fileIdx}/properties")
    @Operation(summary = "Get file properties", description = "Returns file or folder metadata and properties.")
    public ResponseEntity<FileInfoDto.FolderPropertyRes> getFolderProperties(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileInfoService.getFolderProperties(userIdx, fileIdx));
    }

    @GetMapping("/storage/summary")
    @Operation(summary = "Get storage summary", description = "Returns storage usage summary for the current user.")
    public ResponseEntity<FileInfoDto.StorageSummaryRes> getStorageSummary(
            @AuthenticationPrincipal AuthUserDetails dto
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileInfoService.getStorageSummary(userIdx));
    }

    @GetMapping("/{fileIdx}/text-preview")
    @Operation(summary = "Get text preview", description = "Returns a text preview for a supported file.")
    public ResponseEntity<FileInfoDto.TextPreviewRes> getTextPreview(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileInfoService.getTextPreview(userIdx, fileIdx));
    }
}
