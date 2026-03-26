package com.example.WaffleBear.file.service;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.file.manage.dto.FileManageDto;

import java.util.List;

public interface FileUpDownloadService {
    List<FileCommonDto.FileListItemRes> fileList(Long idx);

    FileCommonDto.FileListPageRes fileListPage(Long userIdx, FileManageDto.ListPageReq request);

    FileCommonDto.FileListItemRes createFolder(Long userIdx, FileManageDto.FolderReq request);

    FileCommonDto.FileActionRes moveToTrash(Long userIdx, Long fileIdx);

    FileCommonDto.FileActionRes restoreFromTrash(Long userIdx, Long fileIdx);

    FileCommonDto.FileActionRes deletePermanently(Long userIdx, Long fileIdx);

    FileCommonDto.FileActionRes clearTrash(Long userIdx);

    FileCommonDto.FileActionRes moveToFolder(Long userIdx, Long fileIdx, Long targetParentId);

    FileCommonDto.FileListItemRes renameFolder(Long userIdx, Long folderIdx, String folderName);

    FileInfoDto.FolderPropertyRes getFolderProperties(Long userIdx, Long folderIdx);

    FileCommonDto.FileActionRes moveFilesToFolder(Long userIdx, List<Long> fileIdxList, Long targetParentId);

    FileCommonDto.FileActionRes restoreFilesFromTrash(Long userIdx, List<Long> fileIdxList);

    FileInfoDto.StorageSummaryRes getStorageSummary(Long userIdx);

    FileInfoDto.TextPreviewRes getTextPreview(Long userIdx, Long fileIdx);

    FileCommonDto.FileActionRes setLockedFiles(Long userIdx, List<Long> fileIdxList, boolean locked);

    FileCommonDto.FileDownloadPayload downloadFile(Long userIdx, Long fileIdx);

    String getDownloadUrl(Long userIdx, Long fileIdx);
}
