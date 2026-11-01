package ijiri.ijiriserver.domain.upload.application.port.in;

public interface ReceiveDbImageFileUseCase {

    /**
     * 업로드 URL 의 서명을 확인하고 파일을 저장한다.
     */
    void receive(String imageKey, long expires, String signature, String contentType, byte[] content);
}
