package ijiri.ijiriserver.global.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 저장소의 이미지 키와 공개 URL 을 서로 바꾼다. 저장소를 S3 등으로 옮기면 public-base-url 만 CDN 주소로 바꾼다.
 */
@Component
public class ImageUrlResolver {

    private static final String PATH = "/images/";

    private final String urlPrefix;

    public ImageUrlResolver(@Value("${storage.public-base-url}") String publicBaseUrl) {
        this.urlPrefix = publicBaseUrl + PATH;
    }

    public String urlOf(String imageKey) {
        return urlPrefix + imageKey;
    }

    // 소셜 프로필 사진처럼 우리 저장소가 아닌 URL 이면 비어 있다
    public Optional<String> keyOf(String url) {
        if (url == null || !url.startsWith(urlPrefix)) {
            return Optional.empty();
        }
        return Optional.of(url.substring(urlPrefix.length()));
    }
}
