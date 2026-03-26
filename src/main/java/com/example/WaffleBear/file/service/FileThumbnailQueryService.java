package com.example.WaffleBear.file.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.share.ShareRepository;
import com.example.WaffleBear.file.share.model.FileShare;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileThumbnailQueryService {

    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final ShareRepository shareRepository;
    private final FileThumbnailService fileThumbnailService;

    public FileThumbnailService.ThumbnailPayload loadOwnedThumbnail(Long userIdx, Long fileIdx) {
        requireAuthenticated(userIdx);
        if (fileIdx == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        FileInfo file = fileUpDownloadRepository.findByIdxAndUser_Idx(fileIdx, userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));
        return fileThumbnailService.loadThumbnail(file);
    }

    public FileThumbnailService.ThumbnailPayload loadSharedThumbnail(Long userIdx, Long fileIdx) {
        requireAuthenticated(userIdx);
        if (fileIdx == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        FileShare share = shareRepository.findByFile_IdxAndRecipient_Idx(fileIdx, userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));
        return fileThumbnailService.loadThumbnail(share.getFile());
    }

    private void requireAuthenticated(Long userIdx) {
        if (userIdx == null || userIdx <= 0L) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }
}
