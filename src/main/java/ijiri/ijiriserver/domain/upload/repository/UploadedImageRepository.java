package ijiri.ijiriserver.domain.upload.repository;

import ijiri.ijiriserver.domain.upload.entity.UploadStatus;
import ijiri.ijiriserver.domain.upload.entity.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {

    Optional<UploadedImage> findByImageKey(String imageKey);

    List<UploadedImage> findAllByImageKeyIn(Collection<String> imageKeys);

    List<UploadedImage> findAllByMemberId(Long memberId);

    List<UploadedImage> findAllByStatusNotAndCreatedAtBefore(UploadStatus status, LocalDateTime cutoff);
}
