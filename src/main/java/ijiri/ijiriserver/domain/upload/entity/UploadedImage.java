package ijiri.ijiriserver.domain.upload.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업로드 URL 을 발급한 사진 키. 앱이 파일을 올리면 UPLOADED, 게시물·프로필에 쓰이면 ATTACHED 가 되고,
 * 10분 안에 연결되지 않으면 UploadedImageCleanupScheduler 가 파일까지 지운다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "uploaded_image",
        uniqueConstraints = @UniqueConstraint(name = "uk_uploaded_image_image_key", columnNames = "image_key"),
        indexes = {
                @Index(name = "idx_uploaded_image_member_id", columnList = "member_id"),
                @Index(name = "idx_uploaded_image_status_created_at", columnList = "status, created_at")
        }
)
public class UploadedImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "image_key", nullable = false, length = 100, updatable = false)
    private String imageKey;

    @Column(name = "content_type", nullable = false, length = 30, updatable = false)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UploadStatus status;

    public void markUploaded() {
        this.status = UploadStatus.UPLOADED;
    }

    public void attach() {
        this.status = UploadStatus.ATTACHED;
    }

    public boolean isUploaded() {
        return status == UploadStatus.UPLOADED;
    }
}
