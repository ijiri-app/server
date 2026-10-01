package ijiri.ijiriserver.domain.post.entity;

import ijiri.ijiriserver.domain.upload.dto.ImageInfo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시물 사진. 업로드된 사진의 url, 크기를 복사해 두고 displayOrder 순서로 보여준다(0 이 피드 썸네일).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "post_image",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_image_uploaded_image_id",
                columnNames = "uploaded_image_id"
        ),
        indexes = @Index(name = "idx_post_image_post_id", columnList = "post_id")
)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private Post post;

    @Column(name = "uploaded_image_id", nullable = false, updatable = false)
    private Long uploadedImageId;

    @Column(name = "url", nullable = false, length = 500, updatable = false)
    private String url;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder.Default
    @OneToMany(mappedBy = "postImage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostPartTag> tags = new ArrayList<>();

    public static PostImage of(Post post, ImageInfo image, int displayOrder) {
        return PostImage.builder()
                .post(post)
                .uploadedImageId(image.id())
                .url(image.url())
                .width(image.width())
                .height(image.height())
                .displayOrder(displayOrder)
                .build();
    }

    public void changeDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void clearTags() {
        tags.clear();
    }

    public void addTag(Long partId, double x, double y) {
        tags.add(PostPartTag.builder()
                .postImage(this)
                .partId(partId)
                .x(x)
                .y(y)
                .build()
        );
    }
}
