package ijiri.ijiriserver.domain.post.entity;

import ijiri.ijiriserver.domain.part.entity.PartCategory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시물에 달린 부품(postPartId). 위시리스트는 이 id 를 담는다. 분류는 작성자가 고른 값.
 * 태그는 0개 이상이고 여러 사진에 걸쳐 있을 수 있다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "post_part",
        indexes = {
                @Index(name = "idx_post_part_post_id", columnList = "post_id"),
                @Index(name = "idx_post_part_part_id", columnList = "part_id")
        }
)
public class PostPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    private Post post;

    @Column(name = "part_id", nullable = false, updatable = false)
    private Long partId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PartCategory category;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder.Default
    @OneToMany(mappedBy = "postPart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostPartTag> tags = new ArrayList<>();

    public static PostPart of(Post post, Long partId, PartCategory category, int displayOrder) {
        return PostPart.builder()
                .post(post)
                .partId(partId)
                .category(category)
                .displayOrder(displayOrder)
                .build();
    }

    public void reorder(PartCategory category, int displayOrder) {
        this.category = category;
        this.displayOrder = displayOrder;
    }

    public void clearTags() {
        tags.clear();
    }

    public void addTag(PostImage image, double x, double y) {
        tags.add(PostPartTag.builder()
                .postPart(this)
                .postImage(image)
                .x(x)
                .y(y)
                .build()
        );
    }
}
