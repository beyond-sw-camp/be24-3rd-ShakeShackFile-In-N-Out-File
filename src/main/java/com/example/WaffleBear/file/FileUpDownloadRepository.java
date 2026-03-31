package com.example.WaffleBear.file;

import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileNodeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FileUpDownloadRepository extends JpaRepository<FileInfo, Long>, JpaSpecificationExecutor<FileInfo> {
    interface UserDashboardStatProjection {
        Long getUserIdx();
        Long getFileCount();
        Long getFolderCount();
        Long getUsedBytes();
        Long getSharedFileCount();
    }

    interface StorageScopeStatProjection {
        Boolean getTrashed();
        Long getFileCount();
        Long getFolderCount();
        Long getUsedBytes();
    }

    interface FileFormatStatProjection {
        String getFileFormat();
        Long getFileCount();
        Long getSizeBytes();
    }

    @EntityGraph(attributePaths = {"parent"})
    List<FileInfo> findAllByUser_IdxOrderByLastModifyDateDescUploadDateDesc(Long userIdx);

    @EntityGraph(attributePaths = {"parent"})
    List<FileInfo> findAllByUser_Idx(Long userIdx);

    @EntityGraph(attributePaths = {"parent"})
    Optional<FileInfo> findByIdxAndUser_Idx(Long idx, Long userIdx);

    Optional<FileInfo> findByUser_IdxAndFileSavePath(Long userIdx, String fileSavePath);
    Optional<FileInfo> findByUser_IdxAndParentIsNullAndFileOriginNameAndTrashedFalse(Long userIdx, String fileOriginName);
    Optional<FileInfo> findByUser_IdxAndParent_IdxAndFileOriginNameAndTrashedFalse(Long userIdx, Long parentIdx, String fileOriginName);

    @EntityGraph(attributePaths = {"parent"})
    List<FileInfo> findAllByUser_IdxAndTrashedFalse(Long userIdx);

    @EntityGraph(attributePaths = {"parent"})
    List<FileInfo> findAllByUser_IdxAndTrashedTrue(Long userIdx);

    @EntityGraph(attributePaths = {"parent"})
    List<FileInfo> findByUser_IdxAndTrashedFalseAndNodeTypeOrderByFileSizeDescLastModifyDateDesc(
            Long userIdx,
            FileNodeType nodeType,
            Pageable pageable
    );

    @Query("""
            select coalesce(sum(coalesce(f.fileSize, 0)), 0)
            from FileInfo f
            where f.user.idx = :userIdx
              and (f.nodeType is null or f.nodeType = :fileNodeType)
            """)
    Long sumStoredFileBytesByUser(
            @Param("userIdx") Long userIdx,
            @Param("fileNodeType") FileNodeType fileNodeType
    );

    @Query("""
            select
                coalesce(f.trashed, false) as trashed,
                sum(case when f.nodeType = :folderType then 0 else 1 end) as fileCount,
                sum(case when f.nodeType = :folderType then 1 else 0 end) as folderCount,
                sum(case when f.nodeType = :folderType then 0 else coalesce(f.fileSize, 0) end) as usedBytes
            from FileInfo f
            where f.user.idx = :userIdx
            group by coalesce(f.trashed, false)
            """)
    List<StorageScopeStatProjection> aggregateStorageScopeStatsByUser(
            @Param("userIdx") Long userIdx,
            @Param("folderType") FileNodeType folderType
    );

    @Query("""
            select
                lower(f.fileFormat) as fileFormat,
                count(f) as fileCount,
                sum(coalesce(f.fileSize, 0)) as sizeBytes
            from FileInfo f
            where f.user.idx = :userIdx
              and (f.trashed is null or f.trashed = false)
              and (f.nodeType is null or f.nodeType = :fileNodeType)
            group by lower(f.fileFormat)
            order by lower(f.fileFormat)
            """)
    List<FileFormatStatProjection> aggregateActiveFileFormatStatsByUser(
            @Param("userIdx") Long userIdx,
            @Param("fileNodeType") FileNodeType fileNodeType
    );

    @Query("""
            select
                f.user.idx as userIdx,
                sum(case when f.nodeType = :folderType then 0 else 1 end) as fileCount,
                sum(case when f.nodeType = :folderType then 1 else 0 end) as folderCount,
                sum(case when f.nodeType = :folderType then 0 else coalesce(f.fileSize, 0) end) as usedBytes,
                sum(case when f.nodeType = :folderType then 0 when f.sharedFile = true then 1 else 0 end) as sharedFileCount
            from FileInfo f
            where f.user.idx is not null
            group by f.user.idx
            """)
    List<UserDashboardStatProjection> aggregateDashboardStatsByUser(@Param("folderType") FileNodeType folderType);

    @Query("""
            select
                f.user.idx as userIdx,
                sum(case when f.nodeType = :folderType then 0 else 1 end) as fileCount,
                sum(case when f.nodeType = :folderType then 1 else 0 end) as folderCount,
                sum(case when f.nodeType = :folderType then 0 else coalesce(f.fileSize, 0) end) as usedBytes,
                sum(case when f.nodeType = :folderType then 0 when f.sharedFile = true then 1 else 0 end) as sharedFileCount
            from FileInfo f
            where f.user.idx = :userIdx
            group by f.user.idx
            """)
    Optional<UserDashboardStatProjection> aggregateDashboardStatsForUser(
            @Param("userIdx") Long userIdx,
            @Param("folderType") FileNodeType folderType
    );

    @Query("""
            select distinct lower(f.fileFormat)
            from FileInfo f
            where f.user.idx = :userIdx
              and (f.trashed is null or f.trashed = false)
              and (f.nodeType is null or f.nodeType = :fileNodeType)
              and (
                    (:parentId is null and f.parent is null)
                    or (:parentId is not null and f.parent.idx = :parentId)
              )
            order by lower(f.fileFormat)
            """)
    List<String> findDistinctFileFormatsByUserAndParent(
            @Param("userIdx") Long userIdx,
            @Param("parentId") Long parentId,
            @Param("fileNodeType") FileNodeType fileNodeType
    );
}
