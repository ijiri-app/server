package ijiri.ijiriserver.domain.upload.scheduler;

import ijiri.ijiriserver.domain.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 업로드 URL 만 받고 올리지 않았거나, 올리고 10분 안에 게시물·프로필에 쓰지 않은(작성 취소 등) 사진 정리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadedImageCleanupScheduler {

    private final UploadService uploadService;

    @Scheduled(fixedDelay = 300_000)
    public void deleteUnattached() {
        int deleted = uploadService.deleteUnattached();
        if (deleted > 0) {
            log.info("Deleted {} unattached uploaded images", deleted);
        }
    }
}
