package ijiri.ijiriserver.domain.upload.entity

import ijiri.ijiriserver.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * DB 에 저장한 사진 파일 본문 (DbImageStorageClient 전용). 공용 저장소로 옮기면 이 테이블은 쓰지 않는다.
 */
@Entity
@Table(
    name = "image_file",
    uniqueConstraints = [UniqueConstraint(name = "uk_image_file_image_key", columnNames = ["image_key"])],
)
class ImageFile(
    imageKey: String,
    contentType: String,
    content: ByteArray,
    id: Long = 0L,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = id
        protected set

    @Column(name = "image_key", nullable = false, length = 100, updatable = false)
    var imageKey: String = imageKey
        protected set

    @Column(name = "content_type", nullable = false, length = 30, updatable = false)
    var contentType: String = contentType
        protected set

    @Column(name = "content", nullable = false, updatable = false)
    var content: ByteArray = content
        protected set
}
