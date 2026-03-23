package com.example.WaffleBear.file.lock;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.service.FileUpDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LockService {

    private final FileUpDownloadService fileUpDownloadService;

    public FileCommonDto.FileActionRes setLockedFiles(Long userIdx, List<Long> fileIdxList, boolean locked) {
        return fileUpDownloadService.setLockedFiles(userIdx, fileIdxList, locked);
    }
}
