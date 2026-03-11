package com.example.WaffleBear.file.service;

import com.example.WaffleBear.file.model.FileInfoDto;

import java.util.List;

public interface FileUpDownloadService {
    List<FileInfoDto.FileRes> fileUpload(List<FileInfoDto.FileReq> requests);

    FileInfoDto.CompleteRes completeUpload(FileInfoDto.CompleteReq request);

    FileInfoDto.FileRes fileDownload(FileInfoDto.FileReq dto);

    List<FileInfoDto.FileListItemRes> fileList(Long idx);

    FileInfoDto.FileListItemRes createFolder(FileInfoDto.FolderReq request);

    FileInfoDto.FileActionRes moveToTrash(Long userIdx, Long fileIdx);

    FileInfoDto.FileActionRes deletePermanently(Long userIdx, Long fileIdx);

    FileInfoDto.FileActionRes clearTrash(Long userIdx);
}
