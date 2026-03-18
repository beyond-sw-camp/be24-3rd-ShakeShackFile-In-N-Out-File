package com.example.WaffleBear.workspace.asset;

import com.example.WaffleBear.workspace.asset.model.WorkspaceAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceAssetRepository extends JpaRepository<WorkspaceAsset, Long> {
    List<WorkspaceAsset> findAllByWorkspace_IdxOrderByCreatedAtDesc(Long workspaceIdx);

    List<WorkspaceAsset> findAllByWorkspace_Idx(Long workspaceIdx);

    Optional<WorkspaceAsset> findByIdxAndWorkspace_Idx(Long assetIdx, Long workspaceIdx);
}
