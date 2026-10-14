package ijiri.ijiriserver.domain.carmodel.entity;

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
 * 차종 마스터의 1단계(모델). 브랜드가 판매하는 이름 기준이라 아반떼 N 과 아반떼 N 라인, M3 와 3시리즈는 별개다.
 * 관심 차종과 피드 탭이 이 단위로 묶인다. isCore 는 온보딩에서 위에 보여줄 핵심 계열.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(
        name = "car_model",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_car_model_brand_name",
                columnNames = {"brand", "name"}
        )
)
public class CarModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand", nullable = false, length = 50)
    private String brand;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_core", nullable = false)
    private boolean core;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // 숨긴 차종은 목록·검색에 나오지 않는다 (이미 연결된 게시물·보유 차량은 그대로)
    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    // 목록에 보이는 순서 (CSV 에 처음 나온 순서)
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
