package ijiri.ijiriserver.domain.upload.service.impl;

import ijiri.ijiriserver.domain.upload.client.DbImageStorageClient;
import ijiri.ijiriserver.domain.upload.dto.ImageFileContent;
import ijiri.ijiriserver.domain.upload.entity.UploadStatus;
import ijiri.ijiriserver.domain.upload.entity.UploadedImage;
import ijiri.ijiriserver.domain.upload.exception.UploadStatusCode;
import ijiri.ijiriserver.domain.upload.repository.UploadedImageRepository;
import ijiri.ijiriserver.domain.upload.service.DbImageFileService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "db", matchIfMissing = true)
public class DbImageFileServiceImpl implements DbImageFileService {

    private final UploadedImageRepository uploadedImageRepository;
    private final DbImageStorageClient dbImageStorageClient;

    // R2 처럼 서명된 키·Content-Type 으로 한 번만 받는다. 이미지인지는 게시물에 연결할 때 확인한다
    @Override
    @Transactional
    public void receive(String imageKey, long expires, String signature, String contentType, byte[] content) {
        UploadedImage image = uploadedImageRepository.findByImageKey(imageKey)
                .filter(found -> found.getStatus() == UploadStatus.PENDING)
                .filter(found -> found.getContentType().equals(contentType))
                .filter(found -> dbImageStorageClient.isValidSignature(imageKey, contentType, expires, signature))
                .orElseThrow(() -> new CustomException(UploadStatusCode.INVALID_UPLOAD_URL));
        dbImageStorageClient.save(imageKey, contentType, content);
        image.markUploaded();
    }

    @Override
    @Transactional(readOnly = true)
    public ImageFileContent load(String imageKey) {
        return dbImageStorageClient.load(imageKey)
                .map(file -> new ImageFileContent(file.getContentType(), file.getContent()))
                .orElseThrow(() -> new CustomException(UploadStatusCode.IMAGE_NOT_FOUND));
    }
}
