package ijiri.ijiriserver.domain.upload.application.port.in;

import java.util.Collection;

public interface DeleteUploadedImagesUseCase {

    /**
     * 업로드 기록과 파일을 지운다 (영구 키). 없는 키는 무시한다.
     */
    void delete(Collection<String> imageKeys);
}
