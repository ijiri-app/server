package ijiri.ijiriserver.domain.upload.service;

import ijiri.ijiriserver.domain.upload.dto.ImageInfo;
import ijiri.ijiriserver.domain.upload.dto.response.UploadResponse;

import java.util.Collection;
import java.util.List;

public interface UploadService {

    UploadResponse uploadImage(Long memberId, byte[] content);

    /**
     * 이 회원이 올렸고 아직 쓰이지 않은 사진만 붙일 수 있다 (아니면 UPLOAD4002). 요청 순서대로 돌려준다.
     */
    List<ImageInfo> attach(Long memberId, List<Long> imageIds);

    /**
     * 사진 행과 파일을 지운다. 없는 id 는 무시한다.
     */
    void delete(Collection<Long> imageIds);

    /**
     * 하루가 지나도록 게시물·프로필에 쓰이지 않은 사진을 지우고 지운 수를 돌려준다.
     */
    int deleteUnattached();
}
