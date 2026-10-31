package ijiri.ijiriserver.domain.upload.adapter.out.persistence;

import ijiri.ijiriserver.domain.upload.application.port.out.UploadedImagePort;
import ijiri.ijiriserver.domain.upload.entity.UploadStatus;
import ijiri.ijiriserver.domain.upload.entity.UploadedImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UploadedImagePersistenceAdapter implements UploadedImagePort {

    private final UploadedImageJpaRepository uploadedImageJpaRepository;

    @Override
    public Optional<UploadedImage> findByImageKey(String imageKey) {
        return uploadedImageJpaRepository.findByImageKey(imageKey);
    }

    @Override
    public List<UploadedImage> findAllByImageKeyIn(Collection<String> imageKeys) {
        return uploadedImageJpaRepository.findAllByImageKeyIn(imageKeys);
    }

    @Override
    public List<UploadedImage> findAllByMemberId(Long memberId) {
        return uploadedImageJpaRepository.findAllByMemberId(memberId);
    }

    @Override
    public List<UploadedImage> findAllByStatusNotAndCreatedAtBefore(UploadStatus status, LocalDateTime cutoff) {
        return uploadedImageJpaRepository.findAllByStatusNotAndCreatedAtBefore(status, cutoff);
    }

    @Override
    public void deleteAll(Iterable<UploadedImage> entities) {
        uploadedImageJpaRepository.deleteAll(entities);
    }

    @Override
    public UploadedImage save(UploadedImage entity) {
        return uploadedImageJpaRepository.save(entity);
    }
}
