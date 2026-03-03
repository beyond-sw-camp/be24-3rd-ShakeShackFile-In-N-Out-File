package com.example.WaffleBear.file;

import com.example.WaffleBear.file.model.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileUpDownloadRepository extends JpaRepository<FileInfo, Long> {

}
