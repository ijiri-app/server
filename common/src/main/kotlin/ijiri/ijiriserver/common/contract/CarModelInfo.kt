package ijiri.ijiriserver.common.contract

/**
 * service-car 의 GET /internal/car-models 응답. hidden 인 차종은 새로 고를 수 없지만 이미 연결된 곳에는 계속 보인다.
 */
data class CarModelInfo(
    val id: Long,
    val brand: String,
    val name: String,
    val hidden: Boolean,
)
