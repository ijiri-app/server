package ijiri.ijiriserver.media.upload.entity

import ijiri.ijiriserver.common.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 업로드 URL 을 발급한 사진. 게시물·프로필에 연결되면 ATTACHED 가 되고 imageKey 가 영구 키(tmp/ 제외)로 바뀐다.
 * 하루 안에 연결되지 않으면 UploadedImageCleanupScheduler 가 파일까지 지운다.
 */
@Entity
@Table(
    name = "uploaded_image",
    uniqueConstraints = [UniqueConstraint(name = "uk_uploaded_image_image_key", columnNames = ["image_key"])],
    indexes = [
        Index(name = "idx_uploaded_image_member_id", columnList = "member_id"),
        Index(name = "idx_uploaded_image_status_created_at", columnList = "status, created_at"),
    ],
)
class UploadedImage(
    memberId: Long,
    imageKey: String,
    contentType: String,
    status: UploadStatus,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "member_id", nullable = false, updatable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "image_key", nullable = false, length = 100)
    var imageKey: String = imageKey
        protected set

    @Column(name = "content_type", nullable = false, length = 30, updatable = false)
    var contentType: String = contentType
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: UploadStatus = status
        protected set

    fun markUploaded() {
        this.status = UploadStatus.UPLOADED
    }

    fun attach(permanentKey: String) {
        this.imageKey = permanentKey
        this.status = UploadStatus.ATTACHED
    }

    fun isAttached(): Boolean {
        return status == UploadStatus.ATTACHED
    }
}
