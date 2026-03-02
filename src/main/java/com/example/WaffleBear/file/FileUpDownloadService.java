package com.example.WaffleBear.file;

import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileInfoDto;

public interface FileUpDownloadService {
    FileInfoDto.FileRes fileUpload(FileInfoDto.FileReq dto);
    FileInfoDto.FileRes fileDownload(FileInfoDto.FileReq dto);
    FileInfoDto.FileRes fileList(Long idx);

}
