package ijiri.ijiriserver.domain.upload.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DB 에 저장한 사진 파일 본문 (DbImageStorageClient 전용). 공용 저장소로 옮기면 이 테이블은 쓰지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "image_file",
        uniqueConstraints = @UniqueConstraint(name = "uk_image_file_image_key", columnNames = "image_key")
)
public class ImageFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_key", nullable = false, length = 100, updatable = false)
    private String imageKey;

    @Column(name = "content_type", nullable = false, length = 30, updatable = false)
    private String contentType;

    @Column(name = "content", nullable = false, updatable = false)
    private byte[] content;
}
