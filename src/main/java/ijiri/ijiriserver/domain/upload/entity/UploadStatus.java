package ijiri.ijiriserver.domain.upload.entity;

public enum UploadStatus {
    // 업로드 URL 만 발급됨 (R2 는 업로드 완료를 서버에 알리지 않으므로 연결할 때까지 이 상태다)
    PENDING,
    // DB 저장소로 파일이 올라옴
    UPLOADED,
    // 게시물·프로필에 연결되어 영구 키로 옮겨짐. 이 상태가 아니면 하루 뒤 지운다
    ATTACHED
}
