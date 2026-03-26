package com.example.WaffleBear.file.service;

import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.file.manage.dto.FileManageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@Primary
@RequiredArgsConstructor
public class FileUpDownloadS3Service implements FileUpDownloadService {

    private static final String DEFAULT_PROVIDER = "minio";

    private final FileUpDownloadMinioService minioService;
    private final Environment environment;

    @Override
    public List<FileCommonDto.FileListItemRes> fileList(Long idx) {
        return resolveDelegate().fileList(idx);
    }

    @Override
    public FileCommonDto.FileListPageRes fileListPage(Long userIdx, FileManageDto.ListPageReq request) {
        return resolveDelegate().fileListPage(userIdx, request);
    }

    @Override
    public FileCommonDto.FileListItemRes createFolder(Long userIdx, FileManageDto.FolderReq request) {
        return resolveDelegate().createFolder(userIdx, request);
    }

    @Override
    public FileCommonDto.FileActionRes moveToTrash(Long userIdx, Long fileIdx) {
        return resolveDelegate().moveToTrash(userIdx, fileIdx);
    }

    @Override
    public FileCommonDto.FileActionRes restoreFromTrash(Long userIdx, Long fileIdx) {
        return resolveDelegate().restoreFromTrash(userIdx, fileIdx);
    }

    @Override
    public FileCommonDto.FileActionRes deletePermanently(Long userIdx, Long fileIdx) {
        return resolveDelegate().deletePermanently(userIdx, fileIdx);
    }

    @Override
    public FileCommonDto.FileActionRes clearTrash(Long userIdx) {
        return resolveDelegate().clearTrash(userIdx);
    }

    @Override
    public FileCommonDto.FileActionRes moveToFolder(Long userIdx, Long fileIdx, Long targetParentId) {
        return resolveDelegate().moveToFolder(userIdx, fileIdx, targetParentId);
    }

    @Override
    public FileCommonDto.FileListItemRes renameFolder(Long userIdx, Long folderIdx, String folderName) {
        return resolveDelegate().renameFolder(userIdx, folderIdx, folderName);
    }

    @Override
    public FileInfoDto.FolderPropertyRes getFolderProperties(Long userIdx, Long folderIdx) {
        return resolveDelegate().getFolderProperties(userIdx, folderIdx);
    }

    @Override
    public FileCommonDto.FileActionRes moveFilesToFolder(Long userIdx, List<Long> fileIdxList, Long targetParentId) {
        return resolveDelegate().moveFilesToFolder(userIdx, fileIdxList, targetParentId);
    }

    @Override
    public FileCommonDto.FileActionRes restoreFilesFromTrash(Long userIdx, List<Long> fileIdxList) {
        return resolveDelegate().restoreFilesFromTrash(userIdx, fileIdxList);
    }

    @Override
    public FileInfoDto.StorageSummaryRes getStorageSummary(Long userIdx) {
        return resolveDelegate().getStorageSummary(userIdx);
    }

    @Override
    public FileInfoDto.TextPreviewRes getTextPreview(Long userIdx, Long fileIdx) {
        return resolveDelegate().getTextPreview(userIdx, fileIdx);
    }

    @Override
    public FileCommonDto.FileActionRes setLockedFiles(Long userIdx, List<Long> fileIdxList, boolean locked) {
        return resolveDelegate().setLockedFiles(userIdx, fileIdxList, locked);
    }

    @Override
    public FileCommonDto.FileDownloadPayload downloadFile(Long userIdx, Long fileIdx) {
        return resolveDelegate().downloadFile(userIdx, fileIdx);
    }

    @Override
    public String getDownloadUrl(Long userIdx, Long fileIdx) {
        return resolveDelegate().getDownloadUrl(userIdx, fileIdx);
    }

    private FileUpDownloadService resolveDelegate() {
        String provider = resolveStorageProvider();

        if ("s3".equals(provider) || "minio".equals(provider)) {
            // Current upload/download implementation is S3-compatible through the existing object-storage flow.
            // By keeping the delegate behind this router, we can switch providers from configuration without
            // changing the services that depend on FileUpDownloadService.
            return minioService;
        }

        return minioService;
    }

    private String resolveStorageProvider() {
        return firstNonBlank(
                environment.getProperty("file.storage.provider"),
                environment.getProperty("storage.provider"),
                environment.getProperty("FILE_STORAGE_PROVIDER"),
                environment.getProperty("STORAGE_PROVIDER"),
                System.getProperty("file.storage.provider"),
                System.getProperty("storage.provider"),
                System.getenv("FILE_STORAGE_PROVIDER"),
                System.getenv("STORAGE_PROVIDER"),
                DEFAULT_PROVIDER
        ).trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return DEFAULT_PROVIDER;
        }

        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }

        return DEFAULT_PROVIDER;
    }
}
