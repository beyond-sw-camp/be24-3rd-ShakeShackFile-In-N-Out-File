package com.example.WaffleBear.workspace.asset;

import com.example.WaffleBear.workspace.asset.model.WorkspaceAsset;
import com.example.WaffleBear.workspace.asset.model.WorkspaceAssetType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkspaceAssetRepository extends JpaRepository<WorkspaceAsset, Long> {
    interface UserStoredBytesProjection {
        Long getUserIdx();
        Long getStoredBytes();
    }

    interface WorkspaceAssetView {
        Long getIdx();
        Long getWorkspaceIdx();
        WorkspaceAssetType getAssetType();
        String getOriginalName();
        String getStoredFileName();
        String getObjectFolder();
        String getObjectKey();
        String getContentType();
        Long getFileSize();
        LocalDateTime getCreatedAt();
    }

    List<WorkspaceAsset> findAllByWorkspace_IdxOrderByCreatedAtDesc(Long workspaceIdx);

    List<WorkspaceAsset> findAllByWorkspace_Idx(Long workspaceIdx);

    Optional<WorkspaceAsset> findByIdxAndWorkspace_Idx(Long assetIdx, Long workspaceIdx);

    @Query("""
            select
                a.objectKey
            from WorkspaceAsset a
            where a.workspace.idx = :workspaceIdx
            """)
    List<String> findObjectKeysByWorkspaceIdx(@Param("workspaceIdx") Long workspaceIdx);

    @Query("""
            select
                a.idx as idx,
                a.workspace.idx as workspaceIdx,
                a.assetType as assetType,
                a.originalName as originalName,
                a.storedFileName as storedFileName,
                a.objectFolder as objectFolder,
                a.objectKey as objectKey,
                a.contentType as contentType,
                a.fileSize as fileSize,
                a.createdAt as createdAt
            from WorkspaceAsset a
            where a.workspace.idx = :workspaceIdx
            order by a.createdAt desc
            """)
    List<WorkspaceAssetView> findProjectedAllByWorkspaceIdxOrderByCreatedAtDesc(@Param("workspaceIdx") Long workspaceIdx);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete
            from WorkspaceAsset a
            where a.workspace.idx = :workspaceIdx
            """)
    int deleteAllByWorkspaceIdx(@Param("workspaceIdx") Long workspaceIdx);

    @Query("""
            select
                a.uploader.idx as userIdx,
                sum(coalesce(a.fileSize, 0)) as storedBytes
            from WorkspaceAsset a
            where a.uploader.idx is not null
            group by a.uploader.idx
            """)
    List<UserStoredBytesProjection> aggregateStoredBytesByUploader();
}
