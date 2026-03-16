package com.example.WaffleBear.file.info;

import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.file.service.FileUpDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileInfoService {

    private final FileUpDownloadService fileUpDownloadService;

    public FileInfoDto.FolderPropertyRes getFolderProperties(Long userIdx, Long fileIdx) {
        return fileUpDownloadService.getFolderProperties(userIdx, fileIdx);
    }

    public FileInfoDto.StorageSummaryRes getStorageSummary(Long userIdx) {
        return fileUpDownloadService.getStorageSummary(userIdx);
    }

    public FileInfoDto.TextPreviewRes getTextPreview(Long userIdx, Long fileIdx) {
        return fileUpDownloadService.getTextPreview(userIdx, fileIdx);
    }
}
