package com.example.WaffleBear.file.service;

import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.FileUpDownloadService;
import com.example.WaffleBear.file.model.FileInfoDto;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
//@Service
public class FileUpDownloadLocalService implements FileUpDownloadService {
    private final FileUpDownloadRepository fileUpDownloadRepository;

    @Override
    public FileInfoDto.FileRes fileUpload(FileInfoDto.FileReq dto) {
        return null;
    }

    @Override
    public FileInfoDto.FileRes fileDownload(FileInfoDto.FileReq dto) {
        return null;
    }

    @Override
    public FileInfoDto.FileRes fileList(Long idx) {
        return null;
    }
}
