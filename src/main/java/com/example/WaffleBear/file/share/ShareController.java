package com.example.WaffleBear.file.share;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.file.share.model.ShareDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file/share")
public class ShareController {

    private final ShareService shareService;

    @GetMapping("/shared/list")
    public ResponseEntity<List<ShareDto.SharedFileRes>> sharedFileList(
            @AuthenticationPrincipal AuthUserDetails userDetails) {
        return ResponseEntity.ok(shareService.sharedFileList(userDetails != null ? userDetails.getIdx() : 0L));
    }

    @GetMapping("/{fileIdx}")
    public ResponseEntity<List<ShareDto.ShareInfoRes>> getShareInfo(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long fileIdx) {
        return ResponseEntity.ok(shareService.getShareInfo(userDetails != null ? userDetails.getIdx() : 0L, fileIdx));
    }

    @PostMapping
    public ResponseEntity<FileCommonDto.FileActionRes> shareFiles(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody ShareDto.ShareReq request) {
        return ResponseEntity.ok(shareService.shareFiles(
                userDetails != null ? userDetails.getIdx() : 0L,
                request != null ? request.fileIdxList() : null,
                request != null ? request.recipientEmail() : null
        ));
    }

    @PostMapping("/cancel")
    public ResponseEntity<FileCommonDto.FileActionRes> cancelShare(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @RequestBody ShareDto.ShareReq request) {
        return ResponseEntity.ok(shareService.cancelShare(
                userDetails != null ? userDetails.getIdx() : 0L,
                request != null ? request.fileIdxList() : null,
                request != null ? request.recipientEmail() : null
        ));
    }

    @PostMapping("/shared/{fileIdx}/save")
    public ResponseEntity<FileCommonDto.FileListItemRes> saveSharedFileToDrive(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long fileIdx,
            @RequestBody(required = false) ShareDto.SaveToDriveReq request) {
        return ResponseEntity.ok(shareService.saveSharedFileToDrive(
                userDetails != null ? userDetails.getIdx() : 0L,
                fileIdx,
                request != null ? request.parentId() : null
        ));
    }

    @GetMapping("/shared/{fileIdx}/text-preview")
    public ResponseEntity<FileInfoDto.TextPreviewRes> getSharedTextPreview(
            @AuthenticationPrincipal AuthUserDetails userDetails,
            @PathVariable Long fileIdx) {
        return ResponseEntity.ok(shareService.getSharedTextPreview(
                userDetails != null ? userDetails.getIdx() : 0L,
                fileIdx
        ));
    }
}

