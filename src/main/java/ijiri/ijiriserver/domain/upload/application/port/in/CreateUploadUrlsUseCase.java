package ijiri.ijiriserver.domain.upload.application.port.in;

import ijiri.ijiriserver.domain.upload.application.data.request.UploadUrlRequest;
import ijiri.ijiriserver.domain.upload.application.data.response.UploadResponse;

public interface CreateUploadUrlsUseCase {

    UploadResponse createUploadUrls(Long memberId, UploadUrlRequest request);
}
