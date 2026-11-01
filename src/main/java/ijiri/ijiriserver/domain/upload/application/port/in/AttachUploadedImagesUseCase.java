package ijiri.ijiriserver.domain.upload.application.port.in;

import ijiri.ijiriserver.domain.upload.application.data.AttachedImage;
import ijiri.ijiriserver.domain.upload.entity.UploadPurpose;

import java.util.List;

public interface AttachUploadedImagesUseCase {

    /**
     * 이 회원이 이 용도로 받은 업로드 키이고, 저장소에 10MB 이하의 JPEG·WebP 파일이 올라와 있어야 한다
     * (아니면 INVALID_IMAGE). 영구 키로 옮기고 요청 순서대로 돌려준다.
     */
    List<AttachedImage> attach(Long memberId, List<String> imageKeys, UploadPurpose purpose);
}
