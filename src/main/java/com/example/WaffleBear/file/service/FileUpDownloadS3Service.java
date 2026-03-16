package com.example.WaffleBear.file.service;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.file.manage.dto.FileManageDto;
import lombok.RequiredArgsConstructor;

import java.util.List;

//@Service
//@Primary
@RequiredArgsConstructor
public class FileUpDownloadS3Service implements FileUpDownloadService {

    @Override
    public List<FileCommonDto.FileListItemRes> fileList(Long idx) {
        return List.of();
    }

    @Override
    public FileCommonDto.FileListItemRes createFolder(Long userIdx, FileManageDto.FolderReq request) {
        throw new UnsupportedOperationException("S3 service currently does not support folder operations.");
    }

    @Override
    public FileCommonDto.FileActionRes moveToTrash(Long userIdx, Long fileIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support trash operations.");
    }

    @Override
    public FileCommonDto.FileActionRes restoreFromTrash(Long userIdx, Long fileIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support restore operations.");
    }

    @Override
    public FileCommonDto.FileActionRes deletePermanently(Long userIdx, Long fileIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support permanent delete operations.");
    }

    @Override
    public FileCommonDto.FileActionRes clearTrash(Long userIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support trash operations.");
    }

    @Override
    public FileCommonDto.FileActionRes moveToFolder(Long userIdx, Long fileIdx, Long targetParentId) {
        throw new UnsupportedOperationException("S3 service currently does not support move operations.");
    }

    @Override
    public FileCommonDto.FileListItemRes renameFolder(Long userIdx, Long folderIdx, String folderName) {
        throw new UnsupportedOperationException("S3 service currently does not support rename operations.");
    }

    @Override
    public FileInfoDto.FolderPropertyRes getFolderProperties(Long userIdx, Long folderIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support folder property operations.");
    }

    @Override
    public FileCommonDto.FileActionRes moveFilesToFolder(Long userIdx, List<Long> fileIdxList, Long targetParentId) {
        throw new UnsupportedOperationException("S3 service currently does not support batch move operations.");
    }

    @Override
    public FileCommonDto.FileActionRes restoreFilesFromTrash(Long userIdx, List<Long> fileIdxList) {
        throw new UnsupportedOperationException("S3 service currently does not support restore operations.");
    }

    @Override
    public FileInfoDto.StorageSummaryRes getStorageSummary(Long userIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support storage summary operations.");
    }

    @Override
    public FileInfoDto.TextPreviewRes getTextPreview(Long userIdx, Long fileIdx) {
        throw new UnsupportedOperationException("S3 service currently does not support text preview operations.");
    }

    @Override
    public FileCommonDto.FileActionRes setLockedFiles(Long userIdx, List<Long> fileIdxList, boolean locked) {
        throw new UnsupportedOperationException("S3 service currently does not support lock operations.");
    }
}
