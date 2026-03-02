package com.example.WaffleBear.file.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;
    // 최초 업로드시 저장되는 파일이름
    private String fileOriginName;
    // 파일 저장 기준 이름 ( 이 값 기준으로 파일 디렉터리 구분 )
    private String fileSaveName;
    // 파일 저장 형식( .png, .jpeg, jpg, .zip 등 )
    private String fileFormat;
    // 파일 사이즈 바이트 단위로 저장하고  MB, GB단위로 계산
    private Long fileSize;
    // 파일 업로드 시간( 완료 기준으로 )
    private Date UploadDate;
    // 파일 마지막 수정일( 문서 등 )
    private Date lastModifyDate;
    // 파일 주인
    private String fileOwner;
    // 사용하지 않도록 잠가놓은 파일
    private Boolean lockedtFile;
    // 공유된 파일
    private Boolean sharedFile;


}
