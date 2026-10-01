package ijiri.ijiriserver.domain.upload.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadPurpose {
    POST("posts/tmp/"),
    PROFILE("profiles/tmp/");

    // 저장소 키 앞부분. 용도가 다른 곳에 쓰이지 않도록 연결할 때 확인한다
    private final String keyPrefix;
}
