package ijiri.ijiriserver.domain.upload.application.service;

import ijiri.ijiriserver.domain.upload.application.data.ImageFileContent;
import ijiri.ijiriserver.domain.upload.application.port.in.LoadImageFileUseCase;
import ijiri.ijiriserver.domain.upload.application.port.in.ReceiveImageFileUseCase;
import ijiri.ijiriserver.domain.upload.application.port.out.DbImageStoragePort;
import ijiri.ijiriserver.domain.upload.application.port.out.UploadedImagePort;
import ijiri.ijiriserver.domain.upload.entity.UploadStatus;
import ijiri.ijiriserver.domain.upload.entity.UploadedImage;
import ijiri.ijiriserver.domain.upload.exception.UploadStatusCode;
import ijiri.ijiriserver.global.exception.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "db", matchIfMissing = true)
public class DbImageFileService implements ReceiveImageFileUseCase,
        LoadImageFileUseCase {

    private final UploadedImagePort uploadedImagePort;
    private final DbImageStoragePort dbImageStoragePort;

    // R2 처럼 서명된 키·Content-Type 으로 한 번만 받는다. 이미지인지는 게시물에 연결할 때 확인한다
    @Override
    @Transactional
    public void receive(String imageKey, long expires, String signature, String contentType, byte[] content) {
        UploadedImage image = uploadedImagePort.findByImageKey(imageKey)
                .filter(found -> found.getStatus() == UploadStatus.PENDING)
                .filter(found -> found.getContentType().equals(contentType))
                .filter(found -> dbImageStoragePort.isValidSignature(imageKey, contentType, expires, signature))
                .orElseThrow(() -> new CustomException(UploadStatusCode.INVALID_UPLOAD_URL));
        dbImageStoragePort.save(imageKey, contentType, content);
        image.markUploaded();
    }

    @Override
    @Transactional(readOnly = true)
    public ImageFileContent load(String imageKey) {
        return dbImageStoragePort.load(imageKey)
                .map(file -> new ImageFileContent(file.getContentType(), file.getContent()))
                .orElseThrow(() -> new CustomException(UploadStatusCode.IMAGE_NOT_FOUND));
    }
}
