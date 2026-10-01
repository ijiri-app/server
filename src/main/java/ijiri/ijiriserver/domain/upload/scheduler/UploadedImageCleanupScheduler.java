package ijiri.ijiriserver.domain.upload.scheduler;

import ijiri.ijiriserver.domain.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 올리기만 하고 게시물·프로필에 쓰지 않은(작성 취소 등) 사진 정리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadedImageCleanupScheduler {

    private final UploadService uploadService;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void deleteUnattached() {
        log.info("Deleted {} unattached uploaded images", uploadService.deleteUnattached());
    }
}
