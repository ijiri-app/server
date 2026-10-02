package ijiri.ijiriserver.domain.upload.dto;

/**
 * 저장소에 올라간 파일의 메타데이터 (HEAD).
 */
public record StoredObject(
        long size,
        String contentType
) {
}
