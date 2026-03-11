package com.example.WaffleBear.file.service;

import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

//@Service
@RequiredArgsConstructor
public class FileUpDownloadLocalService implements FileUpDownloadService {
    private static final String LOCAL_ROOT = "M:\\temp";
    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd");

    private final FileUpDownloadRepository fileUpDownloadRepository;

    @Override
    public List<FileInfoDto.FileRes> fileUpload(List<FileInfoDto.FileReq> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Upload request is empty.");
        }

        List<FileInfoDto.FileRes> results = new ArrayList<>();
        for (FileInfoDto.FileReq req : requests) {
            validate(req);

            String originName = sanitizeFileName(req.getFileOriginName());
            String extension = extractExtension(originName);
            String baseName = originName.substring(0, originName.length() - extension.length() - 1);
            Path saveDir = makeDateDirectory(LocalDate.now());
            Path targetPath = resolveUniquePath(saveDir, baseName, extension);

            try {
                Files.createDirectories(saveDir);
                Files.createFile(targetPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save local file metadata target: " + targetPath, e);
            }

            FileInfo entity = FileInfo.builder()
                    .fileOriginName(originName)
                    .fileSaveName(targetPath.toString())
                    .fileFormat(extension)
                    .fileSize(req.getFileSize())
                    .lockedFile(false)
                    .sharedFile(false)
                    .build();
            FileInfo saved = fileUpDownloadRepository.save(entity);

            results.add(buildResponse(saved, targetPath));
        }

        return results;
    }

    @Override
    public FileInfoDto.CompleteRes completeUpload(FileInfoDto.CompleteReq request) {
        return null;
    }

    @Override
    public FileInfoDto.FileRes fileDownload(FileInfoDto.FileReq dto) {
        String targetName = sanitizeFileName(dto == null ? null : dto.getFileOriginName());
        Path filePath = findLatestFileByName(targetName);

        if (filePath == null || !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Download target not found: " + targetName);
        }

        return FileInfoDto.FileRes.builder()
                .fileOriginName(filePath.getFileName().toString())
                .fileSaveName(filePath.toString())
//                .objectUrl(filePath.toUri().toString())
                .presignedUrlExpiresIn(null)
                .build();
    }

    @Override
    public List<FileInfoDto.FileListItemRes> fileList(Long idx) {
        return List.of();
    }

    private FileInfoDto.FileRes buildResponse(FileInfo saved, Path targetPath) {
        return FileInfoDto.FileRes.builder()
//                .fileIdx(saved.getIdx())
                .fileOriginName(saved.getFileOriginName())
                .fileSaveName(saved.getFileSaveName())
//                .objectUrl(targetPath.toUri().toString())
                .presignedUrlExpiresIn(null)
                .build();
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Origin file name is required.");
        }
        String trimmed = fileName.trim();
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name.");
        }
        return trimmed;
    }

    private String extractExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx <= 0 || idx == fileName.length() - 1) {
            throw new IllegalArgumentException("Invalid file extension.");
        }
        return fileName.substring(idx + 1);
    }

    private Path makeDateDirectory(LocalDate date) {
        return Paths.get(
                LOCAL_ROOT,
                date.format(YEAR),
                date.format(MONTH),
                date.format(DAY)
        );
    }

    private Path resolveUniquePath(Path dir, String baseName, String extension) {
        Path candidate = dir.resolve(baseName + "." + extension);
        int index = 1;
        while (Files.exists(candidate)) {
            candidate = dir.resolve(baseName + "(" + index + ")." + extension);
            index++;
        }
        return candidate;
    }

    private Path findLatestFileByName(String fileName) {
        try {
            Path root = Path.of(LOCAL_ROOT);
            if (!Files.exists(root)) {
                return null;
            }

            try (Stream<Path> files = Files.walk(root)) {
                return files
                        .filter(path -> Files.isRegularFile(path))
                        .filter(path -> Objects.equals(path.getFileName().toString(), fileName))
                        .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                        .orElse(null);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to search local file: " + fileName, e);
        }
    }

    private void validate(FileInfoDto.FileReq req) {
        if (req == null) {
            throw new IllegalArgumentException("Upload request is required.");
        }
        sanitizeFileName(req.getFileOriginName());
        if (req.getFileSize() == null || req.getFileSize() <= 0) {
            throw new IllegalArgumentException("File size must be greater than 0.");
        }
        extractExtension(req.getFileOriginName());
    }
}
