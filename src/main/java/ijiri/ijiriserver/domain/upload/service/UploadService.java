package ijiri.ijiriserver.domain.upload.service;

import ijiri.ijiriserver.domain.upload.dto.AttachedImage;
import ijiri.ijiriserver.domain.upload.dto.StoredImage;
import ijiri.ijiriserver.domain.upload.dto.request.UploadUrlRequest;
import ijiri.ijiriserver.domain.upload.dto.response.UploadResponse;
import ijiri.ijiriserver.domain.upload.entity.UploadPurpose;

import java.util.Collection;
import java.util.List;

public interface UploadService {

    UploadResponse createUploadUrls(Long memberId, UploadUrlRequest request);

    /**
     * DB 저장소용: 업로드 URL 의 서명을 확인하고 파일을 저장한다.
     */
    void receiveFile(String imageKey, long expires, String signature, String contentType, byte[] content);

    /**
     * DB 저장소용: 저장된 사진 파일.
     */
    StoredImage loadImage(String imageKey);

    /**
     * 이 회원이 이 용도로 올렸고 아직 쓰이지 않은 사진만 붙일 수 있다 (아니면 INVALID_IMAGE). 요청 순서대로 돌려준다.
     */
    List<AttachedImage> attach(Long memberId, List<String> imageKeys, UploadPurpose purpose);

    /**
     * 업로드 기록과 파일을 지운다. 없는 키는 무시한다.
     */
    void delete(Collection<String> imageKeys);

    /**
     * 10분 안에 게시물·프로필에 연결되지 않은 사진을 지우고 지운 수를 돌려준다.
     */
    int deleteUnattached();
}
