package ijiri.ijiriserver.domain.upload.entity;

public enum UploadStatus {
    // 업로드 URL 만 발급됨
    PENDING,
    // 파일이 저장소에 올라옴
    UPLOADED,
    // 게시물·프로필에 연결됨. 이 상태가 아니면 10분 뒤 지운다
    ATTACHED
}
