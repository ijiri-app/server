package ijiri.ijiriserver.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 저장소의 이미지 키와 공개 URL 을 서로 바꾼다. URL = public-base-url + "/" + 키.
 * 앱이 올리는 임시 키는 tmp/ 로 시작하고, 게시물·프로필에 연결되면 tmp/ 를 뗀 영구 키로 옮겨진다
 * (R2 수명 주기 규칙이 tmp/ 아래만 하루 뒤 지운다).
 */
@Component
public class ImageUrlResolver {

    private static final String TEMPORARY_PREFIX = "tmp/";

    private final String urlPrefix;

    public ImageUrlResolver(@Value("${storage.public-base-url}") String publicBaseUrl) {
        this.urlPrefix = publicBaseUrl + "/";
    }

    public String urlOf(String imageKey) {
        return urlPrefix + imageKey;
    }

    public String permanentKeyOf(String uploadKey) {
        return uploadKey.startsWith(TEMPORARY_PREFIX) ? uploadKey.substring(TEMPORARY_PREFIX.length()) : uploadKey;
    }

    // 소셜 프로필 사진처럼 우리 저장소가 아닌 URL 이면 비어 있다
    public Optional<String> keyOf(String url) {
        if (url == null || !url.startsWith(urlPrefix)) {
            return Optional.empty();
        }
        return Optional.of(url.substring(urlPrefix.length()));
    }
}
