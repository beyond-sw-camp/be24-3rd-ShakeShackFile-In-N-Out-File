package com.example.WaffleBear.file.upload;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileNodeType;
import com.example.WaffleBear.file.upload.dto.UploadDto;
import com.example.WaffleBear.user.model.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadFolderService {

    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final Object folderCreationMonitor = new Object();

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public FileInfo findOrCreateCommittedFolder(Long userIdx, Long parentId, String folderName) {
        synchronized (folderCreationMonitor) {
            return findOrCreateFolder(userIdx, parentId, folderName);
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public FileInfo saveCompletedUpload(
            Long userIdx,
            UploadDto.CompleteReq request,
            String fileOriginName,
            String fileFormat,
            long fileSize,
            String finalObjectKey
    ) {
        synchronized (folderCreationMonitor) {
            Optional<FileInfo> existing = fileUpDownloadRepository.findByUser_IdxAndFileSavePath(userIdx, finalObjectKey);
            if (existing.isPresent()) {
                return existing.get();
            }

            FileInfo parent = resolveUploadParentFolder(userIdx, request != null ? request.getParentId() : null, request != null ? request.getRelativePath() : null, fileOriginName);
            FileInfo entity = FileInfo.builder()
                    .fileOriginName(fileOriginName)
                    .fileFormat(fileFormat)
                    .fileSaveName(extractFileSaveName(finalObjectKey))
                    .fileSavePath(finalObjectKey)
                    .fileSize(fileSize)
                    .nodeType(FileNodeType.FILE)
                    .parent(parent)
                    .lockedFile(false)
                    .sharedFile(false)
                    .trashed(false)
                    .deletedAt(null)
                    .user(User.builder().idx(userIdx).build())
                    .build();

            return fileUpDownloadRepository.saveAndFlush(entity);
        }
    }

    private FileNodeType resolveNodeType(FileInfo entity) {
        return entity.getNodeType() == null ? FileNodeType.FILE : entity.getNodeType();
    }

    private FileInfo resolveUploadParentFolder(Long userIdx, Long parentId, String relativePath, String fileOriginName) {
        FileInfo currentParent = resolveParentFolder(userIdx, parentId);
        List<String> folderSegments = extractFolderSegments(relativePath, fileOriginName);

        for (String folderSegment : folderSegments) {
            currentParent = findOrCreateFolder(userIdx, currentParent != null ? currentParent.getIdx() : null, folderSegment);
        }

        return currentParent;
    }

    private List<String> extractFolderSegments(String relativePath, String fileOriginName) {
        String normalized = normalizeRelativePath(relativePath, fileOriginName);
        String[] pathSegments = normalized.split("/");

        if (pathSegments.length <= 1) {
            return List.of();
        }

        List<String> folderSegments = new ArrayList<>();
        for (int index = 0; index < pathSegments.length - 1; index += 1) {
            String segment = sanitizeFolderName(pathSegments[index]);
            if (!segment.isBlank()) {
                folderSegments.add(segment);
            }
        }

        return folderSegments;
    }

    private FileInfo findOrCreateFolder(Long userIdx, Long parentId, String folderName) {
        Optional<FileInfo> existing = parentId == null
                ? fileUpDownloadRepository.findByUser_IdxAndParentIsNullAndFileOriginNameAndTrashedFalse(userIdx, folderName)
                : fileUpDownloadRepository.findByUser_IdxAndParent_IdxAndFileOriginNameAndTrashedFalse(userIdx, parentId, folderName);

        if (existing.isPresent()) {
            FileInfo folder = existing.get();
            if (resolveNodeType(folder) != FileNodeType.FOLDER) {
                throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
            }
            return folder;
        }

        FileInfo parent = resolveParentFolder(userIdx, parentId);
        FileInfo entity = FileInfo.builder()
                .user(User.builder().idx(userIdx).build())
                .parent(parent)
                .nodeType(FileNodeType.FOLDER)
                .fileOriginName(folderName)
                .fileFormat("folder")
                .fileSaveName("folder-" + UUID.randomUUID())
                .fileSavePath(null)
                .fileSize(0L)
                .lockedFile(false)
                .sharedFile(false)
                .trashed(false)
                .deletedAt(null)
                .build();

        return fileUpDownloadRepository.saveAndFlush(entity);
    }

    private FileInfo resolveParentFolder(Long userIdx, Long parentId) {
        if (parentId == null) {
            return null;
        }

        FileInfo parent = fileUpDownloadRepository.findByIdxAndUser_Idx(parentId, userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));

        if (resolveNodeType(parent) != FileNodeType.FOLDER || parent.isTrashed()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return parent;
    }

    private String sanitizeFolderName(String folderName) {
        String normalized = folderName == null ? "" : folderName.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        if (normalized.contains("..") || normalized.contains("/") || normalized.contains("\\") || normalized.contains("\u0000")) {
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        return normalized;
    }

    private String normalizeRelativePath(String relativePath, String fallbackFileName) {
        String normalized = relativePath == null ? "" : relativePath.trim();
        if (normalized.isEmpty()) {
            return fallbackFileName;
        }
        return normalized.replace("\\", "/");
    }

    private String extractFileSaveName(String finalObjectKey) {
        int separatorIndex = finalObjectKey.lastIndexOf('/');
        if (separatorIndex < 0 || separatorIndex >= finalObjectKey.length() - 1) {
            return finalObjectKey;
        }
        return finalObjectKey.substring(separatorIndex + 1);
    }
}
