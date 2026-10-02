package ijiri.ijiriserver.domain.upload.service;

import ijiri.ijiriserver.domain.upload.dto.AttachedImage;
import ijiri.ijiriserver.domain.upload.dto.request.UploadUrlRequest;
import ijiri.ijiriserver.domain.upload.dto.response.UploadResponse;
import ijiri.ijiriserver.domain.upload.entity.UploadPurpose;

import java.util.Collection;
import java.util.List;

public interface UploadService {

    UploadResponse createUploadUrls(Long memberId, UploadUrlRequest request);

    /**
     * 이 회원이 이 용도로 받은 업로드 키이고, 저장소에 10MB 이하의 JPEG·WebP 파일이 올라와 있어야 한다
     * (아니면 INVALID_IMAGE). 영구 키로 옮기고 요청 순서대로 돌려준다.
     */
    List<AttachedImage> attach(Long memberId, List<String> imageKeys, UploadPurpose purpose);

    /**
     * 업로드 기록과 파일을 지운다 (영구 키). 없는 키는 무시한다.
     */
    void delete(Collection<String> imageKeys);

    /**
     * 하루 안에 게시물·프로필에 연결되지 않은 업로드를 지우고 지운 수를 돌려준다.
     */
    int deleteUnattached();
}
