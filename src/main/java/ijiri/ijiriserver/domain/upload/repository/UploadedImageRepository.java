package ijiri.ijiriserver.domain.upload.repository;

import ijiri.ijiriserver.domain.upload.entity.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {

    List<UploadedImage> findAllByIdInAndMemberIdAndAttachedFalse(Collection<Long> ids, Long memberId);

    Optional<UploadedImage> findByMemberIdAndUrl(Long memberId, String url);

    List<UploadedImage> findAllByMemberId(Long memberId);

    List<UploadedImage> findAllByAttachedFalseAndCreatedAtBefore(LocalDateTime cutoff);
}
