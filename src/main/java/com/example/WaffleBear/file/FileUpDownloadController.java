package com.example.WaffleBear.file;

import com.example.WaffleBear.file.model.FileInfoDto;
import com.example.WaffleBear.file.service.FileUpDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/file")
public class FileUpDownloadController {
    private final FileUpDownloadService fileUpDownloadService;

    @PostMapping("/upload")
    public ResponseEntity<List<FileInfoDto.FileRes>> fileUpload(@RequestBody List<FileInfoDto.FileReq> files){
        List<FileInfoDto.FileRes> result = fileUpDownloadService.fileUpload(files);

        return ResponseEntity.ok(result);
    }

}
