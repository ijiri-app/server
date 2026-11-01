package ijiri.ijiriserver.domain.upload.application.port.out;

import ijiri.ijiriserver.domain.upload.entity.UploadStatus;
import ijiri.ijiriserver.domain.upload.entity.UploadedImage;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UploadedImagePort {

    Optional<UploadedImage> findByImageKey(String imageKey);

    List<UploadedImage> findAllByImageKeyIn(Collection<String> imageKeys);

    List<UploadedImage> findAllByMemberId(Long memberId);

    List<UploadedImage> findAllByStatusNotAndCreatedAtBefore(UploadStatus status, LocalDateTime cutoff);

    void deleteAll(Iterable<UploadedImage> entities);

    UploadedImage save(UploadedImage entity);
}
