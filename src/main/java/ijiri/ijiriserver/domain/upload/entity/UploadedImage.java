package ijiri.ijiriserver.domain.upload.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 회원이 올린 사진. 게시물이나 프로필에 쓰이면 attached 가 되고,
 * 하루가 지나도록 쓰이지 않은 사진은 UploadedImageCleanupScheduler 가 파일까지 지운다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "uploaded_image",
        uniqueConstraints = @UniqueConstraint(name = "uk_uploaded_image_storage_key", columnNames = "storage_key"),
        indexes = {
                @Index(name = "idx_uploaded_image_member_id", columnList = "member_id"),
                @Index(name = "idx_uploaded_image_attached_created_at", columnList = "attached, created_at")
        }
)
public class UploadedImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "storage_key", nullable = false, length = 100, updatable = false)
    private String storageKey;

    @Column(name = "url", nullable = false, length = 500, updatable = false)
    private String url;

    // 서버가 읽지 못하는 형식(HEIC, WebP)이면 null
    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "attached", nullable = false)
    private boolean attached;

    public void attach() {
        this.attached = true;
    }
}
