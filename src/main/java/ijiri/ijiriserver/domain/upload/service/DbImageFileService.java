package ijiri.ijiriserver.domain.upload.service;

import ijiri.ijiriserver.domain.upload.dto.ImageFileContent;

/**
 * DB 저장소(로컬·테스트) 전용: R2 대신 이 서버가 업로드 URL 의 PUT 과 공개 URL 의 GET 을 받는다.
 */
public interface DbImageFileService {

    /**
     * 업로드 URL 의 서명을 확인하고 파일을 저장한다.
     */
    void receive(String imageKey, long expires, String signature, String contentType, byte[] content);

    ImageFileContent load(String imageKey);
}
