package com.example.WaffleBear.administrator.storage;

public enum DataTransferSource {
    DRIVE_UPLOAD("드라이브 업로드"),
    WORKSPACE_UPLOAD("워크스페이스 업로드"),
    CHAT_UPLOAD("채팅 업로드"),
    DRIVE_FILE("드라이브 파일 다운로드"),
    DRIVE_THUMBNAIL("드라이브 썸네일"),
    DRIVE_TEXT_PREVIEW("드라이브 텍스트 미리보기"),
    SHARED_FILE("공유 파일 다운로드"),
    SHARED_THUMBNAIL("공유 파일 썸네일"),
    SHARED_TEXT_PREVIEW("공유 파일 텍스트 미리보기"),
    WORKSPACE_ASSET("워크스페이스 첨부 파일"),
    CHAT_ATTACHMENT("채팅 첨부 파일");

    private final String label;

    DataTransferSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
