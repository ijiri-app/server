package ijiri.ijiriserver.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시물 사진. displayOrder 순서로 보여주고 0 이 피드 썸네일. 크기는 앱이 줄인 뒤 보낸 값이다.
 * 사진은 게시물을 만든 뒤 바꿀 수 없다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "post_image",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_image_image_key", columnNames = "image_key"),
        indexes = @Index(name = "idx_post_image_post_id", columnList = "post_id")
)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private Post post;

    @Column(name = "image_key", nullable = false, length = 100, updatable = false)
    private String imageKey;

    @Column(name = "url", nullable = false, length = 500, updatable = false)
    private String url;

    @Column(name = "width", nullable = false, updatable = false)
    private int width;

    @Column(name = "height", nullable = false, updatable = false)
    private int height;

    @Column(name = "display_order", nullable = false, updatable = false)
    private int displayOrder;
}
