package com.example.WaffleBear.file.service;

import com.example.WaffleBear.common.exception.BaseException;
import com.example.WaffleBear.common.model.BaseResponseStatus;
import com.example.WaffleBear.file.FileUpDownloadRepository;
import com.example.WaffleBear.Config.MinioProperties;
import com.example.WaffleBear.file.model.FileInfo;
import com.example.WaffleBear.file.model.FileInfoDto;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUpDownloadMinioService implements FileUpDownloadService {

    //Todo 0! : 파일 저장 이름, 경로 설정하기, 그리고 DB저장 변수 맞게 조절(DTO, Entity, Service 등)

    private final FileUpDownloadRepository fileUpDownloadRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    private String nowPath = "/";

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024 * 1024; // 5GB

    // @PostConstruct는 스프링이 이 클래스를 다 만들고 나서 앱 시작후 자동으로 1번 실행하라는 것, 서버 켜질때 자동으로 실행되는 초기화 임 그래서 어디서 사용하지 않아도 자동으로 실행 됨
    @PostConstruct
    public void ensureBucketExists() {
        try {
            // 프로퍼티에서 클라우드 버켓 이름 값 가져오기
            String bucket = minioProperties.getBucket_cloud();
            // exists에 미니오 버켓이 있는지 확인 잇으면 참, 없으면 거짓을 거장
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs
                            .builder()
                            .bucket(bucket)
                            .build()
            );
            if (!exists) {
                // 없다면 버킷 생성하기
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("미니오 설정 오류" + minioProperties.getBucket_cloud(), e);
        }
    }

    @Override
    public List<FileInfoDto.FileRes> fileUpload(List<FileInfoDto.FileReq> requests) {
        // 요청 받은게 깡통인지 아닌지 확인
        // 깡통이면 종료
        if (requests == null || requests.isEmpty()) {
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }
        // 요청 개수를 확인
        int size = requests.size();
        if (size > 10) {
            throw BaseException.from(BaseResponseStatus.FILE_COUNT_WRONG);
        }

        // 반환할 결과를 저장할 배열 생성
        List<FileInfoDto.FileRes> result = new ArrayList<>();

        // 요청 받은 수만큼 반복(요청 받은 파일의 수만큼 반복함)
        for (FileInfoDto.FileReq req : requests) {
            // 내부 함수 validate실행 : 변수로 받은 파일의 이상을 확인 ( 파일이 비었는지, 파일 이름이 없는지, 파일의 이름이 너무 긴지, 파일의 사이즈 너무 큰지 )
            validate(req);
            // 오리진 네임 변수에 실제 파일 이름 구하기, 공백 제거
            String fileOriginName = req.getFileOriginName().trim();
            // 포멧 변수에 확장자를 구하기: 확장자를 구하는 함수
            String fileFormat = normalizeFormat(req);
            // 파일이 중복으로 저장안되고 구분해서 저장할 수 있도록 이름 랜덤화 및
            String fileSaveName = buildObjectUrl() + UUID.randomUUID() + "." + fileFormat;

            // String savePath = req.getObjectUrl();

            // DB에 저장될 파일 내용
            // 자동 기입 : IDX, 업로드 주제, 업로드 날짜, 수정날짜
            // 1. 파일 원본 이름
            // 2. 파일 포멧 형식
            // 3. 파일 저장 이름
            // 4. 파일 사이즈
            // 5. 파일 잠금 여부
            // 6. 파일 공유 여부
            FileInfo entity = FileInfo.builder()
                    .fileOriginName(fileOriginName)
                    .fileFormat(fileFormat)
                    .fileSaveName(fileSaveName)
                    .fileSize(req.getFileSize())
                    .lockedFile(false)
                    .sharedFile(false)
                    .build();

            // 완성한 엔티티로 DB에 값 넣기
            FileInfo saved = fileUpDownloadRepository.save(entity);
            // 업로드 URL 발급
            String uploadUrl = generatePresignedUploadUrl(fileSaveName);

            // 클라이언트로 반환할 내용
            // 1. 업로드 된 파일명
            // 2. 실제 저장되는 파일 이름
            // 3. 파일 포멧 형식
            // 4. 파일 업로드 URL
            // 5. 업로드 가능 시간 : 600초
            FileInfoDto.FileRes res = FileInfoDto.FileRes.builder()
                    //.fileIdx(saved.getIdx())
                    .fileOriginName(fileOriginName)
                    .fileSaveName(fileSaveName)
                    .fileFormat(fileFormat)
                    .presignedUploadUrl(uploadUrl)
                    //.objectUrl(buildObjectUrl(fileSaveName))
                    .presignedUrlExpiresIn(minioProperties.getPresignedUrlExpirySeconds())
                    .build();

            result.add(res);
        }

        return result;
    }

    // 업로드 URL 발급 메소드
    private String generatePresignedUploadUrl(String objectName) {
        try {
            // 미니오 클라이언트 내부에 있는 URL 발급 메서드 사용
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioProperties.getBucket_cloud())
                    .object(objectName)
                    .expiry(minioProperties.getPresignedUrlExpirySeconds())
                    .build()
            );
        } catch (Exception ignored) {
            throw BaseException.from(BaseResponseStatus.FILE_UPLOADURL_FAIL);
        }
    }

    // Todo 1: 저장 디렉토리를 확인, 저장하기 위해서인데, 나중에 수정해야할 듯 ver2
    private String buildObjectUrl(String objectName) {
        String endpoint = trimTrailingSlash(minioProperties.getEndpoint());
        return endpoint + "/" + minioProperties.getBucket_cloud() + "/" + objectName;
    }

    // Todo 2: 저장 디렉토리를 확인, 저장하기 위해서인데, 나중에 수정해야할 듯 ver1
    private String buildObjectUrl() {
        String endpoint = trimTrailingSlash(minioProperties.getEndpoint());
        return endpoint + "/" + minioProperties.getBucket_cloud() + "/";
    }

    // 해당 파일의 포멧을 검사와 확인, 수정해서 반환
    private String normalizeFormat(FileInfoDto.FileReq req) {
        // 변수에 파일 확장자 저장, 지정된 파일 확장자가 없는 경우도 있음
        String format = req.getFileFormat();
        // 파일 원본이름 저장
        String originName = req.getFileOriginName();

        // 파일의 확장자를 구하는 장치
        if (format == null || format.isBlank()) {
            // 마지막 .의 위치를 찾고 그걸 기준으로 확장자를 변수에 저장
            int idx = originName.lastIndexOf('.');
            // 확장자가 없을 경우
            if (idx <= 0 || idx >= originName.length() - 1) {
                throw BaseException.from(BaseResponseStatus.FILE_FORMAT_NOTHING);
            }
            // 확장자 구하기
            format = originName.substring(idx + 1);
        }

        // 앞뒤로 쓸데 없는 공백 자르기
        format = format.trim();
        // 맨앞글자가 .이면 점을 때는 장치
        if (format.startsWith(".")) {
            format = format.substring(1);
        }

        // 포멧형식이 안비어야하고, 길이가 20자 이하에, 정규표현식에 맞아야한다. 안그럼 예외처리
        if (format.isEmpty() || format.length() > 20 || !format.matches("^[A-Za-z0-9]+$")) {
            throw BaseException.from(BaseResponseStatus.FILE_FORMAT_WRONG);
        }

        // 확장자 지금까지 처리해서 나온 확장자 반환
        return format.toLowerCase();
    }

    // 파일 상태 검사
    private void validate(FileInfoDto.FileReq req) {
        if (req == null) {
            //throw new IllegalArgumentException("파일이 입력되지 않음");
            throw BaseException.from(BaseResponseStatus.FILE_EMPTY);
        }

        String originName = req.getFileOriginName();
        if (originName == null || originName.isBlank()) {
            //throw new IllegalArgumentException("Origin file name is required.");
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

        if (originName.length() > 100) {
            //throw new IllegalArgumentException("Origin file name is too long.");
            throw BaseException.from(BaseResponseStatus.FILE_NAME_LENGTH_WRONG);
        }

        if (originName.contains("..") || originName.contains("/") || originName.contains("\\") || originName.contains("\u0000")) {
            //throw new IllegalArgumentException("Invalid file name.");
            throw BaseException.from(BaseResponseStatus.FILE_NAME_WRONG);
        }

//        if (req.getFileSize() == null || req.getFileSize() <= 0) {
//            //throw new IllegalArgumentException("File size must be greater than 0.");
//            throw BaseException.from(BaseResponseStatus.FILE_SIZE_WRONG);
//        }
        if (req.getFileSize() > MAX_SIZE_BYTES) {
//            throw new IllegalArgumentException("File size exceeds 5GB.");
            throw BaseException.from(BaseResponseStatus.FILE_SIZE_WRONG);
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }


    // Todo 3: 아직 미구현입니다..
    @Override
    public FileInfoDto.FileRes fileDownload(FileInfoDto.FileReq dto) {
        return null;
    }

    // Todo 4: 아직 미구현입니다..
    @Override
    public FileInfoDto.FileRes fileList(Long idx) {
        return null;
    }
}
