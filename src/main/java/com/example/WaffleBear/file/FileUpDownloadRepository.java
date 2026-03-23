package com.example.WaffleBear.file;

import com.example.WaffleBear.file.model.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileUpDownloadRepository extends JpaRepository<FileInfo, Long> {
    List<FileInfo> findAllByUser_IdxOrderByLastModifyDateDescUploadDateDesc(Long userIdx);
    List<FileInfo> findAllByUser_Idx(Long userIdx);
    Optional<FileInfo> findByIdxAndUser_Idx(Long idx, Long userIdx);
    Optional<FileInfo> findByUser_IdxAndFileSavePath(Long userIdx, String fileSavePath);
    Optional<FileInfo> findByUser_IdxAndParentIsNullAndFileOriginNameAndTrashedFalse(Long userIdx, String fileOriginName);
    Optional<FileInfo> findByUser_IdxAndParent_IdxAndFileOriginNameAndTrashedFalse(Long userIdx, Long parentIdx, String fileOriginName);
}
