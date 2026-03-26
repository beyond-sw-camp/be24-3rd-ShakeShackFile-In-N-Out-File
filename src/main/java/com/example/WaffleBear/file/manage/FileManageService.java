package com.example.WaffleBear.file.manage;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.manage.dto.FileManageDto;
import com.example.WaffleBear.file.service.FileUpDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileManageService {

    private final FileUpDownloadService fileUpDownloadService;

    public List<FileCommonDto.FileListItemRes> list(Long userIdx) {
        return fileUpDownloadService.fileList(userIdx);
    }

    public FileCommonDto.FileListPageRes listPage(Long userIdx, FileManageDto.ListPageReq request) {
        return fileUpDownloadService.fileListPage(userIdx, request);
    }

    public FileCommonDto.FileListItemRes createFolder(Long userIdx, FileManageDto.FolderReq request) {
        return fileUpDownloadService.createFolder(userIdx, request);
    }

    public FileCommonDto.FileActionRes moveToTrash(Long userIdx, Long fileIdx) {
        return fileUpDownloadService.moveToTrash(userIdx, fileIdx);
    }

    public FileCommonDto.FileActionRes restoreFromTrash(Long userIdx, Long fileIdx) {
        return fileUpDownloadService.restoreFromTrash(userIdx, fileIdx);
    }

    public FileCommonDto.FileActionRes deletePermanently(Long userIdx, Long fileIdx) {
        return fileUpDownloadService.deletePermanently(userIdx, fileIdx);
    }

    public FileCommonDto.FileActionRes clearTrash(Long userIdx) {
        return fileUpDownloadService.clearTrash(userIdx);
    }

    public FileCommonDto.FileActionRes moveToFolder(Long userIdx, Long fileIdx, Long targetParentId) {
        return fileUpDownloadService.moveToFolder(userIdx, fileIdx, targetParentId);
    }

    public FileCommonDto.FileListItemRes renameFolder(Long userIdx, Long folderIdx, String folderName) {
        return fileUpDownloadService.renameFolder(userIdx, folderIdx, folderName);
    }

    public FileCommonDto.FileActionRes moveFilesToFolder(Long userIdx, List<Long> fileIdxList, Long targetParentId) {
        return fileUpDownloadService.moveFilesToFolder(userIdx, fileIdxList, targetParentId);
    }

    public FileCommonDto.FileActionRes restoreFilesFromTrash(Long userIdx, List<Long> fileIdxList) {
        return fileUpDownloadService.restoreFilesFromTrash(userIdx, fileIdxList);
    }

    public FileCommonDto.FileDownloadPayload downloadFile(Long userIdx, Long fileIdx) {
        return fileUpDownloadService.downloadFile(userIdx, fileIdx);
    }

    public String getDownloadUrl(Long userIdx, Long fileIdx) {
        return fileUpDownloadService.getDownloadUrl(userIdx, fileIdx);
    }
}
