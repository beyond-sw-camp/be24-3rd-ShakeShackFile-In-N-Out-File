//package com.example.WaffleBear.file;
//
//import com.example.WaffleBear.file.model.FileInfoDto;
//import com.example.WaffleBear.file.service.FileUpDownloadService;
//import com.example.WaffleBear.user.model.AuthUserDetails;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RequiredArgsConstructor
//@RestController
//@RequestMapping("/file")
//public class FileUpDownloadController {
//    private final FileUpDownloadService fileUpDownloadService;
//
//    @PostMapping("/upload")
//    public ResponseEntity<List<FileInfoDto.FileRes>> fileUpload(
//            @AuthenticationPrincipal AuthUserDetails dto,
//            @RequestBody List<FileInfoDto.FileReq> files) {
//        List<FileInfoDto.FileRes> result = fileUpDownloadService.fileUpload(files);
//
//        return ResponseEntity.ok(result);
//    }
//
//    @PostMapping("/upload/complete")
//    public ResponseEntity<FileInfoDto.CompleteRes> completeUpload(
//            @AuthenticationPrincipal AuthUserDetails dto,
//            @RequestBody FileInfoDto.CompleteReq request) {
//        FileInfoDto.CompleteRes result = fileUpDownloadService.completeUpload(request);
//
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping("/list")
//    public ResponseEntity<List<FileInfoDto.FileListItemRes>> fileList(
//            @AuthenticationPrincipal AuthUserDetails dto) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        List<FileInfoDto.FileListItemRes> result = fileUpDownloadService.fileList(userIdx);
//
//        return ResponseEntity.ok(result);
//    }
//
//    @PostMapping("/folder")
//    public ResponseEntity<FileInfoDto.FileListItemRes> createFolder(
//            @RequestBody FileInfoDto.FolderReq request) {
//        FileInfoDto.FileListItemRes result = fileUpDownloadService.createFolder(request);
//
//        return ResponseEntity.ok(result);
//    }
//
//    @PatchMapping("/{fileIdx}/trash")
//    public ResponseEntity<FileInfoDto.FileActionRes> moveToTrash(
//            @AuthenticationPrincipal AuthUserDetails dto,
//            @PathVariable Long fileIdx) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        FileInfoDto.FileActionRes result = fileUpDownloadService.moveToTrash(userIdx, fileIdx);
//
//        return ResponseEntity.ok(result);
//    }
//
//    @DeleteMapping("/{fileIdx}")
//    public ResponseEntity<FileInfoDto.FileActionRes> deletePermanently(
//            @AuthenticationPrincipal AuthUserDetails dto,
//            @PathVariable Long fileIdx) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        FileInfoDto.FileActionRes result = fileUpDownloadService.deletePermanently(userIdx, fileIdx);
//
//        return ResponseEntity.ok(result);
//    }
//
//    @DeleteMapping("/trash")
//    public ResponseEntity<FileInfoDto.FileActionRes> clearTrash(
//            @AuthenticationPrincipal AuthUserDetails dto) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        FileInfoDto.FileActionRes result = fileUpDownloadService.clearTrash(userIdx);
//
//        return ResponseEntity.ok(result);
//    }
//
//    @PatchMapping("/{fileIdx}/move")
//    public ResponseEntity<FileInfoDto.FileActionRes> moveToFolder(
//            @AuthenticationPrincipal AuthUserDetails dto,
//            @PathVariable Long fileIdx,
//            @RequestBody FileInfoDto.MoveReq request) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        FileInfoDto.FileActionRes result = fileUpDownloadService.moveToFolder(
//                userIdx,
//                fileIdx,
//                request != null ? request.getTargetParentId() : null
//        );
//
//        return ResponseEntity.ok(result);
//    }
//
//    @PatchMapping("/{fileIdx}/rename")
//    public ResponseEntity<FileInfoDto.FileListItemRes> renameFolder(
//            @AuthenticationPrincipal AuthUserDetails dto,
//            @PathVariable Long fileIdx,
//            @RequestBody FileInfoDto.RenameReq request) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        FileInfoDto.FileListItemRes result = fileUpDownloadService.renameFolder(
//                userIdx,
//                fileIdx,
//                request != null ? request.getFileName() : null
//        );
//
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping("/{fileIdx}/properties")
//    public ResponseEntity<FileInfoDto.FolderPropertyRes> getFolderProperties(
//            @AuthenticationPrincipal AuthUserDetails dto,
//            @PathVariable Long fileIdx) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        FileInfoDto.FolderPropertyRes result = fileUpDownloadService.getFolderProperties(userIdx, fileIdx);
//
//        return ResponseEntity.ok(result);
//    }
//
//    @PatchMapping("/move")
//    public ResponseEntity<FileInfoDto.FileActionRes> moveFilesToFolder(
//            @AuthenticationPrincipal AuthUserDetails dto,
//            @RequestBody FileInfoDto.MoveBatchReq request) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        FileInfoDto.FileActionRes result = fileUpDownloadService.moveFilesToFolder(
//                userIdx,
//                request != null ? request.getFileIdxList() : null,
//                request != null ? request.getTargetParentId() : null
//        );
//
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping("/storage/summary")
//    public ResponseEntity<FileInfoDto.StorageSummaryRes> getStorageSummary(
//            @AuthenticationPrincipal AuthUserDetails dto) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        FileInfoDto.StorageSummaryRes result = fileUpDownloadService.getStorageSummary(userIdx);
//
//        return ResponseEntity.ok(result);
//    }
//
//    @GetMapping("/{fileIdx}/text-preview")
//    public ResponseEntity<FileInfoDto.TextPreviewRes> getTextPreview(
//            @AuthenticationPrincipal AuthUserDetails dto,
//            @PathVariable Long fileIdx) {
//        Long userIdx = dto != null ? dto.getIdx() : 0L;
//        FileInfoDto.TextPreviewRes result = fileUpDownloadService.getTextPreview(userIdx, fileIdx);
//
//        return ResponseEntity.ok(result);
//    }
//}
