package com.example.WaffleBear.file.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.config.MinioProperties;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.dto.FileCommonDto;
import com.example.WaffleBear.file.info.dto.FileInfoDto;
import com.example.WaffleBear.file.manage.dto.FileManageDto;
import com.example.WaffleBear.file.model.FileNodeType;
import com.example.WaffleBear.file.share.ShareRepository;
import com.example.WaffleBear.user.model.User;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.StatObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class FileUpDownloadMinioService implements FileUpDownloadService {

    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final VideoThumbnailService videoThumbnailService;
    private final StoragePlanService storagePlanService;
    private final ShareRepository shareRepository;

    private static final int MAX_TEXT_PREVIEW_BYTES = 64 * 1024;
    private static final String THUMBNAIL_DIRECTORY_NAME = "thumbnails";
    private final Set<String> thumbnailGenerationInProgress = ConcurrentHashMap.newKeySet();

    @Override
    public List<FileCommonDto.FileListItemRes> fileList(Long idx) {
        Long userIdx = idx == null ? 0L : idx;

        return fileUpDownloadRepository.findAllByUser_IdxOrderByLastModifyDateDescUploadDateDesc(userIdx)
                .stream()
                .map(this::toFileListItem)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileCommonDto.FileListPageRes fileListPage(Long userIdx, FileManageDto.ListPageReq request) {
        Long normalizedUserIdx = userIdx == null ? 0L : userIdx;
        Long parentId = request != null ? request.getParentId() : null;
        int page = sanitizePage(request != null ? request.getPage() : null);
        int size = sanitizePageSize(request != null ? request.getSize() : null);

        if (parentId != null) {
            FileInfo parent = getOwnedFile(normalizedUserIdx, parentId);
            if (parent.isTrashed() || resolveNodeType(parent) != FileNodeType.FOLDER) {
                throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
            }
        }

        Pageable pageable = PageRequest.of(page, size, resolveSort(request != null ? request.getSortOption() : null));
        Page<FileInfo> result = fileUpDownloadRepository.findAll(buildPageSpecification(normalizedUserIdx, request), pageable);

        return FileCommonDto.FileListPageRes.builder()
                .fileList(result.getContent().stream().map(this::toFileListItem).toList())
                .breadcrumbs(buildBreadcrumbs(normalizedUserIdx, parentId))
                .availableExtensions(fileUpDownloadRepository.findDistinctFileFormatsByUserAndParent(
                        normalizedUserIdx,
                        parentId,
                        FileNodeType.FILE
                ))
                .totalPage(result.getTotalPages())
                .totalCount(result.getTotalElements())
                .currentPage(result.getNumber())
                .currentSize(result.getSize())
                .build();
    }

    @Override
    public FileCommonDto.FileListItemRes createFolder(Long userIdx, FileManageDto.FolderReq request) {
        if (request == null || request.getFolderName() == null || request.getFolderName().isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String folderName = sanitizeFolderName(request.getFolderName());
        FileInfo parent = resolveParentFolder(userIdx, request.getParentId());

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

        return toFileListItem(fileUpDownloadRepository.save(entity));
    }

    @Override
    public FileCommonDto.FileActionRes moveToTrash(Long userIdx, Long fileIdx) {
        FileInfo target = getOwnedFile(userIdx, fileIdx);
        List<FileInfo> userFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx);
        List<FileInfo> targetTree = collectTargetTree(target, userFiles);
        ensureNoLockedNodes(targetTree);
        LocalDateTime deletedAt = LocalDateTime.now();

        targetTree.forEach(file -> file.markTrashed(deletedAt));
        fileUpDownloadRepository.saveAll(targetTree);

        return FileCommonDto.FileActionRes.builder()
                .targetIdx(fileIdx)
                .action("trash")
                .affectedCount(targetTree.size())
                .build();
    }

    @Override
    public FileCommonDto.FileActionRes restoreFromTrash(Long userIdx, Long fileIdx) {
        return restoreFilesFromTrash(userIdx, List.of(fileIdx));
    }

    @Override
    public FileCommonDto.FileActionRes deletePermanently(Long userIdx, Long fileIdx) {
        FileInfo target = getOwnedFile(userIdx, fileIdx);
        List<FileInfo> userFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx);
        List<FileInfo> targetTree = collectTargetTree(target, userFiles);
        ensureNoLockedNodes(targetTree);

        removeMinioObjects(targetTree);
        deleteFileShares(targetTree);
        fileUpDownloadRepository.deleteAll(sortForDelete(targetTree));

        return FileCommonDto.FileActionRes.builder()
                .targetIdx(fileIdx)
                .action("permanent-delete")
                .affectedCount(targetTree.size())
                .build();
    }

    @Override
    public FileCommonDto.FileActionRes clearTrash(Long userIdx) {
        List<FileInfo> trashedFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx)
                .stream()
                .filter(FileInfo::isTrashed)
                .toList();

        if (trashedFiles.isEmpty()) {
            return FileCommonDto.FileActionRes.builder()
                    .targetIdx(null)
                    .action("clear-trash")
                    .affectedCount(0)
                    .build();
        }

        ensureNoLockedNodes(trashedFiles);
        removeMinioObjects(trashedFiles);
        deleteFileShares(trashedFiles);
        fileUpDownloadRepository.deleteAll(sortForDelete(trashedFiles));

        return FileCommonDto.FileActionRes.builder()
                .targetIdx(null)
                .action("clear-trash")
                .affectedCount(trashedFiles.size())
                .build();
    }

    @Override
    public FileCommonDto.FileActionRes moveToFolder(Long userIdx, Long fileIdx, Long targetParentId) {
        return moveFilesToFolder(userIdx, List.of(fileIdx), targetParentId);
    }

    @Override
    public FileCommonDto.FileActionRes moveFilesToFolder(Long userIdx, List<Long> fileIdxList, Long targetParentId) {
        if (fileIdxList == null || fileIdxList.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        List<FileInfo> userFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx);
        Map<Long, FileInfo> fileById = userFiles.stream()
                .filter(file -> file.getIdx() != null)
                .collect(HashMap::new, (map, file) -> map.put(file.getIdx(), file), HashMap::putAll);
        FileInfo targetParent = resolveParentFolder(userIdx, targetParentId);

        List<FileInfo> requestedSources = fileIdxList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(fileIdx -> resolveOwnedFile(fileById, userIdx, fileIdx))
                .toList();

        if (requestedSources.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        Set<Long> requestedSourceIds = requestedSources.stream()
                .map(FileInfo::getIdx)
                .filter(Objects::nonNull)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        List<FileInfo> topLevelSources = requestedSources.stream()
                .filter(source -> !hasSelectedAncestor(source, requestedSourceIds, fileById))
                .toList();

        if (targetParent != null) {
            for (FileInfo source : topLevelSources) {
                if (source.getIdx().equals(targetParent.getIdx())) {
                    throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
                }
            }
        }

        for (FileInfo source : topLevelSources) {
            ensureNoLockedNodes(resolveNodeType(source) == FileNodeType.FOLDER ? collectTargetTree(source, userFiles) : List.of(source));

            if (source.isTrashed()) {
                throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
            }

            if (resolveNodeType(source) == FileNodeType.FOLDER && targetParent != null) {
                List<FileInfo> descendants = collectTargetTree(source, userFiles);
                boolean movingIntoOwnTree = descendants.stream()
                        .map(FileInfo::getIdx)
                        .anyMatch(idx -> idx != null && idx.equals(targetParent.getIdx()));

                if (movingIntoOwnTree) {
                    throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
                }
            }
        }

        topLevelSources.forEach(source -> source.changeParent(targetParent));
        fileUpDownloadRepository.saveAll(topLevelSources);

        return FileCommonDto.FileActionRes.builder()
                .targetIdx(targetParentId)
                .action("move")
                .affectedCount(topLevelSources.size())
                .build();
    }

    @Override
    public FileCommonDto.FileActionRes restoreFilesFromTrash(Long userIdx, List<Long> fileIdxList) {
        if (fileIdxList == null || fileIdxList.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        List<FileInfo> userFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx);
        Map<Long, FileInfo> fileById = userFiles.stream()
                .filter(file -> file.getIdx() != null)
                .collect(HashMap::new, (map, file) -> map.put(file.getIdx(), file), HashMap::putAll);

        List<FileInfo> requestedTargets = fileIdxList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(fileIdx -> resolveOwnedFile(fileById, userIdx, fileIdx))
                .toList();

        if (requestedTargets.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        List<FileInfo> topLevelTargets = requestedTargets.stream()
                .filter(target -> target.isTrashed())
                .filter(target -> !hasTrashedAncestor(target, fileById))
                .toList();

        if (topLevelTargets.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        List<FileInfo> restoreTargets = new ArrayList<>();
        Set<Long> restoredIds = new HashSet<>();

        for (FileInfo target : topLevelTargets) {
            validateRestorableParent(target, fileById);
            for (FileInfo node : collectTargetTree(target, userFiles)) {
                if (node.getIdx() != null && restoredIds.add(node.getIdx())) {
                    if (!node.isTrashed()) {
                        continue;
                    }
                    node.restore();
                    restoreTargets.add(node);
                }
            }
        }

        fileUpDownloadRepository.saveAll(restoreTargets);

        return FileCommonDto.FileActionRes.builder()
                .targetIdx(null)
                .action("restore")
                .affectedCount(restoreTargets.size())
                .build();
    }

    @Override
    public FileCommonDto.FileListItemRes renameFolder(Long userIdx, Long folderIdx, String folderName) {
        FileInfo folder = getOwnedFile(userIdx, folderIdx);
        if (folder.isTrashed() || resolveNodeType(folder) != FileNodeType.FOLDER) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        folder.rename(sanitizeFolderName(folderName));
        return toFileListItem(fileUpDownloadRepository.save(folder));
    }

    @Override
    public FileInfoDto.FolderPropertyRes getFolderProperties(Long userIdx, Long folderIdx) {
        FileInfo folder = getOwnedFile(userIdx, folderIdx);
        if (folder.isTrashed() || resolveNodeType(folder) != FileNodeType.FOLDER) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        List<FileInfo> userFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx)
                .stream()
                .filter(file -> !file.isTrashed())
                .toList();
        List<FileInfo> directChildren = userFiles.stream()
                .filter(file -> file.getParent() != null && folderIdx.equals(file.getParent().getIdx()))
                .toList();
        List<FileInfo> targetTree = collectTargetTree(folder, userFiles);
        List<FileInfo> descendants = targetTree.stream()
                .filter(file -> !folderIdx.equals(file.getIdx()))
                .toList();

        int directFileCount = (int) directChildren.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FILE)
                .count();
        int directFolderCount = (int) directChildren.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FOLDER)
                .count();
        int totalFileCount = (int) descendants.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FILE)
                .count();
        int totalFolderCount = (int) descendants.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FOLDER)
                .count();
        long directSize = directChildren.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FILE)
                .map(FileInfo::getFileSize)
                .filter(size -> size != null && size > 0)
                .mapToLong(Long::longValue)
                .sum();
        long totalSize = descendants.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FILE)
                .map(FileInfo::getFileSize)
                .filter(size -> size != null && size > 0)
                .mapToLong(Long::longValue)
                .sum();

        return FileInfoDto.FolderPropertyRes.builder()
                .idx(folder.getIdx())
                .folderName(folder.getFileOriginName())
                .parentId(folder.getParent() != null ? folder.getParent().getIdx() : null)
                .directChildCount(directChildren.size())
                .directFileCount(directFileCount)
                .directFolderCount(directFolderCount)
                .totalChildCount(descendants.size())
                .totalFileCount(totalFileCount)
                .totalFolderCount(totalFolderCount)
                .directSize(directSize)
                .totalSize(totalSize)
                .uploadDate(folder.getUploadDate())
                .lastModifyDate(folder.getLastModifyDate())
                .build();
    }

    @Override
    public FileInfoDto.StorageSummaryRes getStorageSummary(Long userIdx) {
        List<FileInfo> userFiles = fileUpDownloadRepository.findAllByUser_Idx(userIdx);
        List<FileInfo> allFileNodes = userFiles.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FILE)
                .toList();
        List<FileInfo> activeFileNodes = allFileNodes.stream()
                .filter(file -> !file.isTrashed())
                .toList();
        List<FileInfo> trashFileNodes = allFileNodes.stream()
                .filter(FileInfo::isTrashed)
                .toList();
        List<FileInfo> activeFolderNodes = userFiles.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FOLDER && !file.isTrashed())
                .toList();
        List<FileInfo> trashFolderNodes = userFiles.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FOLDER && file.isTrashed())
                .toList();

        StoragePlanService.StorageQuota storageQuota = storagePlanService.resolveQuota(userIdx);
        long quotaBytes = storageQuota.totalQuotaBytes();
        long usedBytes = sumFileSizes(allFileNodes);
        long activeUsedBytes = sumFileSizes(activeFileNodes);
        long trashUsedBytes = sumFileSizes(trashFileNodes);
        int usagePercent = quotaBytes > 0
                ? (int) Math.min(100, Math.round((usedBytes * 100.0) / quotaBytes))
                : 0;
        long remainingBytes = Math.max(0L, quotaBytes - usedBytes);

        Map<String, StorageCategoryAccumulator> categories = new HashMap<>();
        initializeStorageCategories(categories);
        for (FileInfo file : activeFileNodes) {
            String categoryKey = categorizeExtension(file.getFileFormat());
            StorageCategoryAccumulator accumulator = categories.get(categoryKey);
            if (accumulator == null) {
                continue;
            }

            accumulator.count += 1;
            accumulator.sizeBytes += safeFileSize(file);
        }

        List<FileInfoDto.StorageCategoryRes> categoryResponses = List.of(
                toStorageCategoryResponse("document", categories.get("document"), activeUsedBytes),
                toStorageCategoryResponse("image", categories.get("image"), activeUsedBytes),
                toStorageCategoryResponse("video", categories.get("video"), activeUsedBytes),
                toStorageCategoryResponse("archive", categories.get("archive"), activeUsedBytes),
                toStorageCategoryResponse("audio", categories.get("audio"), activeUsedBytes),
                toStorageCategoryResponse("other", categories.get("other"), activeUsedBytes)
        );

        List<FileInfoDto.StorageTopFileRes> largestFiles = activeFileNodes.stream()
                .sorted(Comparator.comparingLong(this::safeFileSize).reversed()
                        .thenComparing(FileInfo::getLastModifyDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(7)
                .map(this::toStorageTopFile)
                .toList();

        return FileInfoDto.StorageSummaryRes.builder()
                .planCode(storageQuota.planCode())
                .planLabel(storageQuota.planLabel())
                .adminAccount(storagePlanService.isAdministrator(userIdx))
                .shareEnabled(storageQuota.shareEnabled())
                .fileLockEnabled(storageQuota.fileLockEnabled())
                .maxUploadFileBytes(storageQuota.maxUploadFileBytes())
                .maxUploadCount(storageQuota.maxUploadCount())
                .quotaBytes(quotaBytes)
                .baseQuotaBytes(storageQuota.baseQuotaBytes())
                .addonQuotaBytes(storageQuota.addonQuotaBytes())
                .usedBytes(usedBytes)
                .activeUsedBytes(activeUsedBytes)
                .trashUsedBytes(trashUsedBytes)
                .remainingBytes(remainingBytes)
                .usagePercent(usagePercent)
                .totalFileCount(allFileNodes.size())
                .activeFileCount(activeFileNodes.size())
                .trashFileCount(trashFileNodes.size())
                .activeFolderCount(activeFolderNodes.size())
                .trashFolderCount(trashFolderNodes.size())
                .categories(categoryResponses)
                .largestFiles(largestFiles)
                .build();
    }

    @Override
    public FileInfoDto.TextPreviewRes getTextPreview(Long userIdx, Long fileIdx) {
        FileInfo file = getOwnedFile(userIdx, fileIdx);
        if (resolveNodeType(file) != FileNodeType.FILE || file.isLockedFile()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        if (!isTextPreviewable(file.getFileFormat())) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String objectKey = file.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        try (var objectStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucket_cloud())
                        .object(objectKey)
                        .build()
        )) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int totalRead = 0;
            boolean truncated = false;
            int read;

            while ((read = objectStream.read(buffer)) != -1) {
                int writable = Math.min(read, MAX_TEXT_PREVIEW_BYTES - totalRead);
                if (writable > 0) {
                    outputStream.write(buffer, 0, writable);
                    totalRead += writable;
                }

                if (totalRead >= MAX_TEXT_PREVIEW_BYTES) {
                    truncated = true;
                    break;
                }
            }

            return FileInfoDto.TextPreviewRes.builder()
                    .idx(file.getIdx())
                    .fileOriginName(file.getFileOriginName())
                    .fileFormat(file.getFileFormat())
                    .contentType(resolveTextContentType(file.getFileFormat()))
                    .content(outputStream.toString(StandardCharsets.UTF_8))
                    .truncated(truncated)
                    .fileSize(file.getFileSize())
                    .build();
        } catch (Exception e) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    @Override
    public FileCommonDto.FileDownloadPayload downloadFile(Long userIdx, Long fileIdx) {
        FileInfo file = getOwnedFile(userIdx, fileIdx);
        if (resolveNodeType(file) != FileNodeType.FILE || file.isLockedFile()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String objectKey = file.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return new FileCommonDto.FileDownloadPayload(
                readObjectBytes(minioProperties.getBucket_cloud(), objectKey),
                resolveDownloadContentType(file.getFileOriginName()),
                sanitizeDownloadFileName(file.getFileOriginName(), file.getFileSaveName()),
                file.getFileSize()
        );
    }

    @Override
    public String getDownloadUrl(Long userIdx, Long fileIdx) {
        FileInfo file = getOwnedFile(userIdx, fileIdx);
        if (resolveNodeType(file) != FileNodeType.FILE || file.isLockedFile()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        String objectKey = file.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return generateAttachmentDownloadUrl(
                objectKey,
                sanitizeDownloadFileName(file.getFileOriginName(), file.getFileSaveName()),
                resolveDownloadContentType(file.getFileOriginName())
        );
    }

    private FileInfo resolveParentFolder(Long userIdx, Long parentId) {
        if (parentId == null) {
            return null;
        }

        FileInfo parent = getOwnedFile(userIdx, parentId);
        if (resolveNodeType(parent) != FileNodeType.FOLDER || parent.isTrashed()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return parent;
    }

    private FileInfo getOwnedFile(Long userIdx, Long fileIdx) {
        if (userIdx == null || fileIdx == null) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        return fileUpDownloadRepository.findByIdxAndUser_Idx(fileIdx, userIdx)
                .orElseThrow(() -> BaseException.from(BaseResponseStatus.REQUEST_ERROR));
    }

    private FileInfo resolveOwnedFile(Map<Long, FileInfo> fileById, Long userIdx, Long fileIdx) {
        FileInfo file = fileById.get(fileIdx);
        if (file != null) {
            return file;
        }

        return getOwnedFile(userIdx, fileIdx);
    }

    private List<FileInfo> collectTargetTree(FileInfo target, List<FileInfo> userFiles) {
        Map<Long, List<FileInfo>> childrenByParent = new HashMap<>();
        for (FileInfo file : userFiles) {
            Long parentId = file.getParent() != null ? file.getParent().getIdx() : null;
            if (parentId == null) {
                continue;
            }
            childrenByParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(file);
        }

        List<FileInfo> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<FileInfo> queue = new ArrayDeque<>();
        queue.add(target);

        while (!queue.isEmpty()) {
            FileInfo current = queue.removeFirst();
            if (current.getIdx() == null || !visited.add(current.getIdx())) {
                continue;
            }

            result.add(current);
            for (FileInfo child : childrenByParent.getOrDefault(current.getIdx(), List.of())) {
                queue.addLast(child);
            }
        }

        return result;
    }

    private List<FileInfo> sortForDelete(List<FileInfo> targetTree) {
        Map<Long, FileInfo> fileById = targetTree.stream()
                .filter(file -> file.getIdx() != null)
                .collect(HashMap::new, (map, file) -> map.put(file.getIdx(), file), HashMap::putAll);

        return targetTree.stream()
                .sorted(Comparator.comparingInt((FileInfo file) -> calculateDepth(file, fileById)).reversed())
                .toList();
    }

    private int calculateDepth(FileInfo file, Map<Long, FileInfo> fileById) {
        int depth = 0;
        Set<Long> visited = new HashSet<>();
        FileInfo current = file.getParent();

        while (current != null && current.getIdx() != null && visited.add(current.getIdx())) {
            depth += 1;
            FileInfo resolved = fileById.get(current.getIdx());
            current = resolved != null ? resolved.getParent() : current.getParent();
        }

        return depth;
    }

    private boolean hasSelectedAncestor(FileInfo file, Set<Long> selectedIds, Map<Long, FileInfo> fileById) {
        FileInfo parent = file.getParent();
        Set<Long> visited = new HashSet<>();

        while (parent != null && parent.getIdx() != null && visited.add(parent.getIdx())) {
            if (selectedIds.contains(parent.getIdx())) {
                return true;
            }

            parent = fileById.getOrDefault(parent.getIdx(), parent).getParent();
        }

        return false;
    }

    private boolean hasTrashedAncestor(FileInfo file, Map<Long, FileInfo> fileById) {
        FileInfo parent = file.getParent();
        Set<Long> visited = new HashSet<>();

        while (parent != null && parent.getIdx() != null && visited.add(parent.getIdx())) {
            FileInfo resolved = fileById.getOrDefault(parent.getIdx(), parent);
            if (resolved.isTrashed()) {
                return true;
            }
            parent = resolved.getParent();
        }

        return false;
    }

    private void validateRestorableParent(FileInfo file, Map<Long, FileInfo> fileById) {
        FileInfo parent = file.getParent();
        if (parent == null || parent.getIdx() == null) {
            return;
        }

        FileInfo resolvedParent = fileById.getOrDefault(parent.getIdx(), parent);
        if (resolvedParent.isTrashed() || resolveNodeType(resolvedParent) != FileNodeType.FOLDER) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    @Override
    public FileCommonDto.FileActionRes setLockedFiles(Long userIdx, List<Long> fileIdxList, boolean locked) {
        if (fileIdxList == null || fileIdxList.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        StoragePlanService.StorageQuota storageQuota = storagePlanService.resolveQuota(userIdx);
        if (locked && !storageQuota.fileLockEnabled()) {
            throw BaseException.from(BaseResponseStatus.PLAN_FEATURE_NOT_AVAILABLE);
        }

        List<FileInfo> files = fileIdxList.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(fileIdx -> getOwnedFile(userIdx, fileIdx))
                .toList();

        for (FileInfo file : files) {
            if (resolveNodeType(file) != FileNodeType.FILE || file.isTrashed()) {
                throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
            }
            file.changeLockedFile(locked);
        }

        fileUpDownloadRepository.saveAll(files);

        return FileCommonDto.FileActionRes.builder()
                .targetIdx(null)
                .action(locked ? "lock" : "unlock")
                .affectedCount(files.size())
                .build();
    }

    private void ensureNoLockedNodes(List<FileInfo> files) {
        boolean hasLockedFile = files.stream()
                .anyMatch(file -> resolveNodeType(file) == FileNodeType.FILE && file.isLockedFile());

        if (hasLockedFile) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private void removeMinioObjects(List<FileInfo> files) {
        List<DeleteObject> deleteTargets = files.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FILE)
                .flatMap(file -> buildFileObjectKeysForRemoval(file).stream())
                .filter(path -> path != null && !path.isBlank())
                .distinct()
                .map(DeleteObject::new)
                .toList();

        if (deleteTargets.isEmpty()) {
            return;
        }

        try {
            Iterable<Result<io.minio.messages.DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .objects(deleteTargets)
                            .build()
            );

            for (Result<io.minio.messages.DeleteError> result : results) {
                result.get();
            }
        } catch (Exception ignored) {
        }
    }

    private void deleteFileShares(List<FileInfo> files) {
        List<Long> fileIds = files.stream()
                .filter(file -> resolveNodeType(file) == FileNodeType.FILE)
                .map(FileInfo::getIdx)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (fileIds.isEmpty()) {
            return;
        }

        List<com.example.WaffleBear.file.share.model.FileShare> shares = shareRepository.findAllByFile_IdxIn(fileIds);
        if (shares.isEmpty()) {
            return;
        }

        shareRepository.deleteAll(shares);
    }

    private void deleteObjectKeys(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }

        try {
            List<DeleteObject> objects = objectKeys.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(key -> !key.isBlank())
                    .distinct()
                    .map(DeleteObject::new)
                    .toList();

            if (objects.isEmpty()) {
                return;
            }

            Iterable<Result<io.minio.messages.DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .objects(objects)
                            .build()
            );

            for (Result<io.minio.messages.DeleteError> result : results) {
                result.get();
            }
        } catch (Exception ignored) {
        }
    }

    private String generatePresignedDownloadUrl(FileInfo entity) {
        if (resolveNodeType(entity) != FileNodeType.FILE || entity.isLockedFile()) {
            return null;
        }

        String objectKey = entity.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket_cloud())
                    .object(objectKey)
                    .expiry(minioProperties.getPresignedUrlExpirySeconds())
                    .build());
        } catch (Exception e) {
            return null;
        }
    }

    private String generateAttachmentDownloadUrl(String objectKey, String fileName, String contentType) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put(
                    "response-content-disposition",
                    ContentDisposition.attachment()
                            .filename(fileName, StandardCharsets.UTF_8)
                            .build()
                            .toString()
            );
            queryParams.put("response-content-type", resolveDownloadContentType(fileName));
            if (contentType != null && !contentType.isBlank()) {
                queryParams.put("response-content-type", contentType);
            }

            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket_cloud())
                    .object(objectKey)
                    .expiry(minioProperties.getPresignedUrlExpirySeconds())
                    .extraQueryParams(queryParams)
                    .build());
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private String generatePresignedThumbnailUrl(FileInfo entity) {
        if (resolveNodeType(entity) != FileNodeType.FILE || entity.isLockedFile() || !isVideoFile(entity.getFileFormat())) {
            return null;
        }

        String objectKey = entity.getFileSavePath();
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String thumbnailObjectKey = buildThumbnailObjectKey(objectKey);
        try {
            if (!objectExists(thumbnailObjectKey)) {
                triggerThumbnailGeneration(entity, thumbnailObjectKey);
                return null;
            }

            return generatePresignedGetUrl(thumbnailObjectKey);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void triggerThumbnailGeneration(FileInfo entity, String thumbnailObjectKey) {
        if (thumbnailObjectKey == null || thumbnailObjectKey.isBlank()) {
            return;
        }

        if (!videoThumbnailService.supports(entity.getFileFormat())) {
            return;
        }

        if (!thumbnailGenerationInProgress.add(thumbnailObjectKey)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                if (!objectExists(thumbnailObjectKey)) {
                    ensureVideoThumbnail(entity, thumbnailObjectKey);
                }
            } finally {
                thumbnailGenerationInProgress.remove(thumbnailObjectKey);
            }
        });
    }

    private void ensureVideoThumbnail(FileInfo entity, String thumbnailObjectKey) {
        String objectKey = entity.getFileSavePath();
        if (objectKey == null || objectKey.isBlank() || !videoThumbnailService.supports(entity.getFileFormat())) {
            return;
        }

        Path tempVideoPath = null;
        try {
            tempVideoPath = Files.createTempFile("wafflebear-video-", "." + entity.getFileFormat());
            try (var objectStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .build()
            )) {
                Files.copy(objectStream, tempVideoPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            byte[] thumbnailBytes = videoThumbnailService.createThumbnail(tempVideoPath);
            if (thumbnailBytes == null || thumbnailBytes.length == 0) {
                return;
            }

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(thumbnailBytes)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucket_cloud())
                                .object(thumbnailObjectKey)
                                .stream(inputStream, thumbnailBytes.length, -1)
                                .contentType("image/jpeg")
                                .build()
                );
            }
        } catch (Exception ignored) {
        } finally {
            if (tempVideoPath != null) {
                try {
                    Files.deleteIfExists(tempVideoPath);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean objectExists(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }

        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket_cloud())
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String generatePresignedGetUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucket_cloud())
                    .object(objectKey)
                    .expiry(minioProperties.getPresignedUrlExpirySeconds())
                    .build());
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> buildFileObjectKeysForRemoval(FileInfo entity) {
        List<String> objectKeys = new ArrayList<>();
        String fileObjectKey = entity.getFileSavePath();
        if (fileObjectKey != null && !fileObjectKey.isBlank()) {
            objectKeys.add(fileObjectKey);
            if (isVideoFile(entity.getFileFormat())) {
                objectKeys.add(buildThumbnailObjectKey(fileObjectKey));
            }
        }

        return objectKeys;
    }

    private String buildThumbnailObjectKey(String objectKey) {
        int pathSeparatorIndex = objectKey.lastIndexOf('/');
        String directory = pathSeparatorIndex >= 0 ? objectKey.substring(0, pathSeparatorIndex + 1) : "";
        String fileName = pathSeparatorIndex >= 0 ? objectKey.substring(pathSeparatorIndex + 1) : objectKey;
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;

        return directory + THUMBNAIL_DIRECTORY_NAME + "/" + baseName + ".jpg";
    }

    private String normalizeFormat(String rawFormat, String originName) {
        String format = rawFormat;
        if (format == null || format.isBlank()) {
            int idx = originName.lastIndexOf('.');
            if (idx <= 0 || idx >= originName.length() - 1) {
                throw BaseException.from(BaseResponseStatus.FILE_FORMAT_NOTHING);
            }
            format = originName.substring(idx + 1);
        }

        format = format.trim();
        if (format.startsWith(".")) {
            format = format.substring(1);
        }

        if (format.isEmpty() || format.length() > 20 || !format.matches("^[A-Za-z0-9]+$")) {
            throw BaseException.from(BaseResponseStatus.FILE_FORMAT_WRONG);
        }

        return format.toLowerCase();
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

    private Specification<FileInfo> buildPageSpecification(Long userIdx, FileManageDto.ListPageReq request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            Long parentId = request != null ? request.getParentId() : null;
            String keyword = normalizeSearchKeyword(request != null ? request.getSearchQuery() : null);
            String extensionFilter = normalizeFilterValue(request != null ? request.getExtensionFilter() : null, "all");
            String sizeFilter = normalizeFilterValue(request != null ? request.getSizeFilter() : null, "all");
            String statusFilter = normalizeFilterValue(request != null ? request.getStatusFilter() : null, "all");

            Predicate isFile = criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("nodeType")),
                    criteriaBuilder.equal(root.get("nodeType"), FileNodeType.FILE)
            );
            Predicate isFolder = criteriaBuilder.equal(root.get("nodeType"), FileNodeType.FOLDER);

            predicates.add(criteriaBuilder.equal(root.get("user").get("idx"), userIdx));
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("trashed")),
                    criteriaBuilder.isFalse(root.get("trashed"))
            ));

            if (parentId == null) {
                predicates.add(criteriaBuilder.isNull(root.get("parent")));
            } else {
                predicates.add(criteriaBuilder.equal(root.get("parent").get("idx"), parentId));
            }

            if (!keyword.isBlank()) {
                String likePattern = "%" + keyword + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fileOriginName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fileFormat")), likePattern)
                ));
            }

            if (!"all".equals(extensionFilter)) {
                predicates.add(criteriaBuilder.and(
                        isFile,
                        criteriaBuilder.equal(criteriaBuilder.lower(root.get("fileFormat")), extensionFilter)
                ));
            }

            if (!"all".equals(sizeFilter)) {
                Expression<Long> fileSize = criteriaBuilder.coalesce(root.get("fileSize"), 0L);
                Predicate sizePredicate = switch (sizeFilter) {
                    case "lte10" -> criteriaBuilder.lessThanOrEqualTo(fileSize, megabytesToBytes(10L));
                    case "lte100" -> criteriaBuilder.lessThanOrEqualTo(fileSize, megabytesToBytes(100L));
                    case "lte1000" -> criteriaBuilder.lessThanOrEqualTo(fileSize, megabytesToBytes(1000L));
                    case "lte100000" -> criteriaBuilder.lessThanOrEqualTo(fileSize, megabytesToBytes(100000L));
                    case "gte100001" -> criteriaBuilder.greaterThanOrEqualTo(fileSize, megabytesToBytes(100001L));
                    case "custom" -> buildCustomSizePredicate(criteriaBuilder, fileSize, request);
                    default -> null;
                };

                if (sizePredicate != null) {
                    predicates.add(criteriaBuilder.or(isFolder, criteriaBuilder.and(isFile, sizePredicate)));
                }
            }

            switch (statusFilter) {
                case "shared", "shared-owned" -> predicates.add(criteriaBuilder.isTrue(root.get("sharedFile")));
                case "shared-with-me" -> predicates.add(criteriaBuilder.disjunction());
                case "locked" -> predicates.add(criteriaBuilder.isTrue(root.get("lockedFile")));
                case "unlocked" -> predicates.add(criteriaBuilder.isFalse(root.get("lockedFile")));
                default -> {
                }
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Predicate buildCustomSizePredicate(
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            Expression<Long> fileSize,
            FileManageDto.ListPageReq request
    ) {
        Long minBytes = parseMegabytesToBytes(request != null ? request.getCustomMinSize() : null);
        Long maxBytes = parseMegabytesToBytes(request != null ? request.getCustomMaxSize() : null);

        if (minBytes == null && maxBytes == null) {
            return null;
        }
        if (minBytes != null && maxBytes != null) {
            return criteriaBuilder.between(fileSize, minBytes, maxBytes);
        }
        if (minBytes != null) {
            return criteriaBuilder.greaterThanOrEqualTo(fileSize, minBytes);
        }
        return criteriaBuilder.lessThanOrEqualTo(fileSize, maxBytes);
    }

    private List<FileCommonDto.FileBreadcrumbRes> buildBreadcrumbs(Long userIdx, Long parentId) {
        if (parentId == null) {
            return List.of();
        }

        List<FileCommonDto.FileBreadcrumbRes> breadcrumbs = new ArrayList<>();
        FileInfo cursor = getOwnedFile(userIdx, parentId);
        if (cursor.isTrashed() || resolveNodeType(cursor) != FileNodeType.FOLDER) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }

        while (cursor != null) {
            breadcrumbs.add(FileCommonDto.FileBreadcrumbRes.builder()
                    .idx(cursor.getIdx())
                    .fileOriginName(cursor.getFileOriginName())
                    .parentId(cursor.getParent() != null ? cursor.getParent().getIdx() : null)
                    .build());
            cursor = cursor.getParent();
        }

        Collections.reverse(breadcrumbs);
        return breadcrumbs;
    }

    private Sort resolveSort(String sortOption) {
        String normalizedSort = normalizeFilterValue(sortOption, "updatedat-desc");

        return switch (normalizedSort) {
            case "updatedat-asc" -> Sort.by(
                    Sort.Order.asc("lastModifyDate"),
                    Sort.Order.asc("uploadDate")
            );
            case "name-asc" -> Sort.by(Sort.Order.asc("fileOriginName").ignoreCase());
            case "name-desc" -> Sort.by(Sort.Order.desc("fileOriginName").ignoreCase());
            case "size-asc" -> Sort.by(Sort.Order.asc("fileSize"), Sort.Order.desc("lastModifyDate"));
            case "size-desc" -> Sort.by(Sort.Order.desc("fileSize"), Sort.Order.desc("lastModifyDate"));
            default -> Sort.by(
                    Sort.Order.desc("lastModifyDate"),
                    Sort.Order.desc("uploadDate")
            );
        };
    }

    private int sanitizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int sanitizePageSize(Integer size) {
        if (size == null || size <= 0) {
            return 10;
        }
        return Math.min(30, size);
    }

    private String normalizeSearchKeyword(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFilterValue(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private long megabytesToBytes(long megabytes) {
        return megabytes * 1024L * 1024L;
    }

    private Long parseMegabytesToBytes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            double megabytes = Double.parseDouble(value.trim());
            if (!Double.isFinite(megabytes) || megabytes < 0) {
                return null;
            }
            return Math.round(megabytes * 1024D * 1024D);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private FileCommonDto.FileListItemRes toFileListItem(FileInfo entity) {
        FileNodeType nodeType = resolveNodeType(entity);

        return FileCommonDto.FileListItemRes.builder()
                .idx(entity.getIdx())
                .fileOriginName(entity.getFileOriginName())
                .fileSaveName(entity.getFileSaveName())
                .fileSavePath(entity.getFileSavePath())
                .fileFormat(entity.getFileFormat())
                .fileSize(entity.getFileSize())
                .nodeType(nodeType.name())
                .parentId(entity.getParent() != null ? entity.getParent().getIdx() : null)
                .lockedFile(entity.isLockedFile())
                .sharedFile(entity.isSharedFile())
                .trashed(entity.isTrashed())
                .deletedAt(entity.getDeletedAt())
                .uploadDate(entity.getUploadDate())
                .lastModifyDate(entity.getLastModifyDate())
                .presignedDownloadUrl(generatePresignedDownloadUrl(entity))
                .thumbnailPresignedUrl(generatePresignedThumbnailUrl(entity))
                .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                .build();
    }
    private long sumFileSizes(List<FileInfo> fileNodes) {
        return fileNodes.stream()
                .mapToLong(this::safeFileSize)
                .sum();
    }

    private long safeFileSize(FileInfo file) {
        return file != null && file.getFileSize() != null && file.getFileSize() > 0
                ? file.getFileSize()
                : 0L;
    }

    private void initializeStorageCategories(Map<String, StorageCategoryAccumulator> categories) {
        categories.put("document", new StorageCategoryAccumulator("문서"));
        categories.put("image", new StorageCategoryAccumulator("이미지"));
        categories.put("video", new StorageCategoryAccumulator("동영상"));
        categories.put("archive", new StorageCategoryAccumulator("압축"));
        categories.put("audio", new StorageCategoryAccumulator("오디오"));
        categories.put("other", new StorageCategoryAccumulator("기타"));
    }
    private String categorizeExtension(String fileFormat) {
        String extension = fileFormat == null ? "" : fileFormat.trim().toLowerCase(Locale.ROOT);

        if (Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "hwp").contains(extension)) {
            return "document";
        }

        if (Set.of("jpg", "jpeg", "png", "gif", "svg", "webp", "bmp", "heic").contains(extension)) {
            return "image";
        }

        if (Set.of("mp4", "mov", "avi", "mkv", "wmv", "webm").contains(extension)) {
            return "video";
        }

        if (Set.of("zip", "rar", "7z", "tar", "gz").contains(extension)) {
            return "archive";
        }

        if (Set.of("mp3", "wav", "aac", "flac", "ogg", "m4a").contains(extension)) {
            return "audio";
        }

        return "other";
    }

    private boolean isVideoFile(String fileFormat) {
        String extension = fileFormat == null ? "" : fileFormat.trim().toLowerCase(Locale.ROOT);
        return Set.of("mp4", "mov", "avi", "mkv", "wmv", "webm", "m4v", "mpeg", "mpg", "ogv", "3gp").contains(extension);
    }

    private boolean isTextPreviewable(String fileFormat) {
        String extension = fileFormat == null ? "" : fileFormat.trim().toLowerCase(Locale.ROOT);

        return Set.of(
                "txt", "md", "csv", "log", "json", "xml", "html", "htm",
                "css", "js", "ts", "java", "py", "sql", "yml", "yaml",
                "properties", "sh", "bat"
        ).contains(extension);
    }

    private String resolveTextContentType(String fileFormat) {
        String extension = fileFormat == null ? "" : fileFormat.trim().toLowerCase(Locale.ROOT);

        if (Set.of("json").contains(extension)) {
            return "application/json";
        }

        if (Set.of("html", "htm").contains(extension)) {
            return "text/html";
        }

        if (Set.of("xml").contains(extension)) {
            return "application/xml";
        }

        if (Set.of("csv").contains(extension)) {
            return "text/csv";
        }

        return "text/plain";
    }

    private byte[] readObjectBytes(String bucketName, String objectKey) {
        try (var objectStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build()
        )) {
            return objectStream.readAllBytes();
        } catch (Exception exception) {
            throw BaseException.from(BaseResponseStatus.REQUEST_ERROR);
        }
    }

    private String resolveDownloadContentType(String fileName) {
        String contentType = URLConnection.guessContentTypeFromName(fileName == null ? "" : fileName);
        return (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
    }

    private String sanitizeDownloadFileName(String preferredName, String fallbackName) {
        String candidate = preferredName;
        if (candidate == null || candidate.isBlank()) {
            candidate = fallbackName;
        }
        if (candidate == null || candidate.isBlank()) {
            candidate = "file";
        }

        return candidate
                .replace("\r", "")
                .replace("\n", "")
                .trim();
    }

    private FileInfoDto.StorageCategoryRes toStorageCategoryResponse(
            String categoryKey,
            StorageCategoryAccumulator accumulator,
            long totalActiveBytes) {
        long sizeBytes = accumulator != null ? accumulator.sizeBytes : 0L;
        int usagePercent = totalActiveBytes > 0
                ? (int) Math.round((sizeBytes * 100.0) / totalActiveBytes)
                : 0;

        return FileInfoDto.StorageCategoryRes.builder()
                .categoryKey(categoryKey)
                .categoryLabel(accumulator != null ? accumulator.label : categoryKey)
                .fileCount(accumulator != null ? accumulator.count : 0)
                .sizeBytes(sizeBytes)
                .usagePercent(usagePercent)
                .build();
    }

    private FileInfoDto.StorageTopFileRes toStorageTopFile(FileInfo entity) {
        return FileInfoDto.StorageTopFileRes.builder()
                .idx(entity.getIdx())
                .fileOriginName(entity.getFileOriginName())
                .fileFormat(entity.getFileFormat())
                .fileSize(entity.getFileSize())
                .lastModifyDate(entity.getLastModifyDate())
                .parentId(entity.getParent() != null ? entity.getParent().getIdx() : null)
                .build();
    }

    private FileNodeType resolveNodeType(FileInfo entity) {
        return entity.getNodeType() == null ? FileNodeType.FILE : entity.getNodeType();
    }

    private static class StorageCategoryAccumulator {
        private final String label;
        private int count;
        private long sizeBytes;

        private StorageCategoryAccumulator(String label) {
            this.label = label;
        }
    }
}
