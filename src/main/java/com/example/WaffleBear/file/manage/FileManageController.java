package com.example.WaffleBear.file.manage;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.manage.dto.FileManageDto;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileManageController {

    private final FileManageService fileManageService;

    @GetMapping("/list")
    public ResponseEntity<List<FileCommonDto.FileListItemRes>> fileList(
            @AuthenticationPrincipal AuthUserDetails dto
    ) {
        // dto확인 즉, 로그인 상태 확인
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.list(userIdx));
    }

    @PostMapping("/folder")
    public ResponseEntity<FileCommonDto.FileListItemRes> createFolder(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody FileManageDto.FolderReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.createFolder(userIdx, request));
    }

    @PatchMapping("/{fileIdx}/trash")
    public ResponseEntity<FileCommonDto.FileActionRes> moveToTrash(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.moveToTrash(userIdx, fileIdx));
    }

    @PatchMapping("/{fileIdx}/restore")
    public ResponseEntity<FileCommonDto.FileActionRes> restoreFromTrash(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.restoreFromTrash(userIdx, fileIdx));
    }

    @DeleteMapping("/{fileIdx}")
    public ResponseEntity<FileCommonDto.FileActionRes> deletePermanently(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.deletePermanently(userIdx, fileIdx));
    }

    @DeleteMapping("/trash")
    public ResponseEntity<FileCommonDto.FileActionRes> clearTrash(
            @AuthenticationPrincipal AuthUserDetails dto
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.clearTrash(userIdx));
    }

    @PatchMapping("/{fileIdx}/move")
    public ResponseEntity<FileCommonDto.FileActionRes> moveToFolder(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx,
            @RequestBody FileManageDto.MoveReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.moveToFolder(
                userIdx,
                fileIdx,
                request != null ? request.getTargetParentId() : null
        ));
    }

    @PatchMapping("/{fileIdx}/rename")
    public ResponseEntity<FileCommonDto.FileListItemRes> renameFolder(
            @AuthenticationPrincipal AuthUserDetails dto,
            @PathVariable Long fileIdx,
            @RequestBody FileManageDto.RenameReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.renameFolder(
                userIdx,
                fileIdx,
                request != null ? request.getFileName() : null
        ));
    }

    @PatchMapping("/move")
    public ResponseEntity<FileCommonDto.FileActionRes> moveFilesToFolder(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody FileManageDto.MoveBatchReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.moveFilesToFolder(
                userIdx,
                request != null ? request.getFileIdxList() : null,
                request != null ? request.getTargetParentId() : null
        ));
    }

    @PatchMapping("/restore")
    public ResponseEntity<FileCommonDto.FileActionRes> restoreFilesFromTrash(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody FileManageDto.RestoreBatchReq request
    ) {
        Long userIdx = dto != null ? dto.getIdx() : 0L;
        return ResponseEntity.ok(fileManageService.restoreFilesFromTrash(
                userIdx,
                request != null ? request.getFileIdxList() : null
        ));
    }
}
