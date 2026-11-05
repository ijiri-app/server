package ijiri.ijiriserver.common.contract

/**
 * service-user 의 GET /internal/owned-cars/{id} 응답. 게시물은 차종·트림·연식을 복사해 피드 필터와 카드에 쓴다.
 */
data class OwnedCarSnapshot(
    val ownedCarId: Long,
    val memberId: Long,
    val spec: CarSpec,
    val modelYear: Int,
)
