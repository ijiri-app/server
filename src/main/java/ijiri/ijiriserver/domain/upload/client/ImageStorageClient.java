package ijiri.ijiriserver.domain.upload.client;

import java.time.Duration;

/**
 * 사진 저장소. 앱이 서버를 거치지 않고 저장소에 직접 올리도록 업로드 URL(presigned URL)을 만든다.
 * 지금은 DB 구현(DbImageStorageClient)만 있고, 배포할 때 S3·GCS 구현으로 바꾼다.
 */
public interface ImageStorageClient {

    String createUploadUrl(String imageKey, String contentType, Duration validity);

    boolean exists(String imageKey);

    /**
     * 없는 파일이면 아무것도 하지 않는다 (재시도해도 안전).
     */
    void delete(String imageKey);
}
