package ijiri.ijiriserver.domain.interestcar.entity;

import ijiri.ijiriserver.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

/**
 * 회원의 관심 차종. displayOrder 는 앱 화면에 보이는 순서(0부터).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "member_interest_car",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_interest_car_member_id_car_model_id",
                columnNames = {"member_id", "car_model_id"}
        ),
        indexes = @Index(name = "idx_member_interest_car_member_id", columnList = "member_id")
)
public class MemberInterestCar extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "car_model_id", nullable = false, updatable = false)
    private Long carModelId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
