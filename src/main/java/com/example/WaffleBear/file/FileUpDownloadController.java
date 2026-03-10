package com.example.WaffleBear.file;

import com.example.WaffleBear.file.model.FileInfoDto;
import com.example.WaffleBear.file.service.FileUpDownloadService;
import com.example.WaffleBear.user.model.AuthUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/file")
public class FileUpDownloadController {
    private final FileUpDownloadService fileUpDownloadService;

    @PostMapping("/upload")
    public ResponseEntity<List<FileInfoDto.FileRes>> fileUpload(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody List<FileInfoDto.FileReq> files) {
        List<FileInfoDto.FileRes> result = fileUpDownloadService.fileUpload(files);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/upload/complete")
    public ResponseEntity<FileInfoDto.CompleteRes> completeUpload(
            @AuthenticationPrincipal AuthUserDetails dto,
            @RequestBody FileInfoDto.CompleteReq request) {
        FileInfoDto.CompleteRes result = fileUpDownloadService.completeUpload(request);

        return ResponseEntity.ok(result);
    }
}
