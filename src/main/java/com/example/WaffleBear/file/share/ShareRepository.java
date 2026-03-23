package com.example.WaffleBear.file.share;

import com.example.WaffleBear.file.share.model.FileShare;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShareRepository extends JpaRepository<FileShare, Long> {

    @EntityGraph(attributePaths = {"file", "owner", "recipient"})
    List<FileShare> findAllByRecipient_IdxOrderByCreatedAtDesc(Long recipientIdx);

    @EntityGraph(attributePaths = {"file", "owner", "recipient"})
    List<FileShare> findAllByOwner_IdxOrderByCreatedAtDesc(Long ownerIdx);

    @EntityGraph(attributePaths = {"file", "owner", "recipient"})
    List<FileShare> findAllByFile_Idx(Long fileIdx);

    @EntityGraph(attributePaths = {"file", "owner", "recipient"})
    List<FileShare> findAllByFile_IdxIn(List<Long> fileIdxList);

    Optional<FileShare> findByFile_IdxAndRecipient_Idx(Long fileIdx, Long recipientIdx);

    Optional<FileShare> findByFile_IdxAndRecipient_Email(Long fileIdx, String recipientEmail);

    long countByFile_Idx(Long fileIdx);
}

