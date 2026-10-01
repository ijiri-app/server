package ijiri.ijiriserver.domain.upload.client;

/**
 * 사진 파일 저장소. 지금은 로컬 디스크 구현만 있고, 서버를 여러 대 띄우면 S3 등 공용 저장소 구현으로 바꾼다.
 */
public interface ImageStorageClient {

    /**
     * 파일을 저장하고 공개 URL 을 돌려준다.
     */
    String store(String key, byte[] content);

    /**
     * 없는 파일이면 아무것도 하지 않는다 (재시도해도 안전).
     */
    void delete(String key);
}
