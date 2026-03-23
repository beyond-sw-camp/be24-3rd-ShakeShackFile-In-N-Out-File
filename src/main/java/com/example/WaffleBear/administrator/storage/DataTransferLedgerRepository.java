package com.example.WaffleBear.administrator.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DataTransferLedgerRepository extends JpaRepository<DataTransferLedger, Long> {
    List<DataTransferLedger> findAllByOrderByCreatedAtDesc();

    List<DataTransferLedger> findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(LocalDateTime createdAt);
}
