package ijiri.ijiriserver.domain.upload.client;

import ijiri.ijiriserver.domain.upload.dto.StoredObject;

import java.time.Duration;
import java.util.Optional;

/**
 * 사진 저장소. 앱이 서버를 거치지 않고 저장소에 직접 올리도록 업로드 URL(presigned URL)을 만들고,
 * 연결할 때 올라온 파일을 확인한다. 운영은 Cloudflare R2(R2ImageStorageClient), 로컬·테스트는 DB(DbImageStorageClient).
 * storage.type 으로 고른다.
 */
public interface ImageStorageClient {

    /**
     * contentType 을 서명에 묶어, 앱은 같은 Content-Type 으로만 올릴 수 있다.
     */
    String createUploadUrl(String imageKey, String contentType, Duration validity);

    Optional<StoredObject> head(String imageKey);

    /**
     * 파일 앞부분 length 바이트 (파일이 더 짧으면 전체).
     */
    byte[] readPrefix(String imageKey, int length);

    void copy(String sourceKey, String targetKey);

    /**
     * 없는 파일이면 아무것도 하지 않는다 (재시도해도 안전).
     */
    void delete(String imageKey);
}
