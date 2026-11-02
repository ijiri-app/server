package ijiri.ijiriserver.domain.upload.adapter.`in`.scheduler

import ijiri.ijiriserver.domain.upload.application.port.`in`.DeleteUnattachedUploadsUseCase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 업로드 URL 만 받고 올리지 않았거나, 올리고 10분 안에 게시물·프로필에 쓰지 않은(작성 취소 등) 사진 정리.
 */
@Component
class UploadedImageCleanupScheduler(
    private val deleteUnattachedUploadsUseCase: DeleteUnattachedUploadsUseCase,
) {

    @Scheduled(fixedDelay = 300_000)
    fun deleteUnattached() {
        val deleted = deleteUnattachedUploadsUseCase.deleteUnattached()
        if (deleted > 0) {
            log.info("Deleted {} unattached uploaded images", deleted)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(UploadedImageCleanupScheduler::class.java)
    }
}
