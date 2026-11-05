package ijiri.ijiriserver.common.contract

/**
 * 트림 하나로 정해지는 모델 / 세대 / 트림 조합. service-car 의 GET /internal/car-trims 응답이며
 * 보유 차량·게시물이 연식 검증(세대 판매 기간)과 표시에 쓴다.
 */
data class CarSpec(
    val carModelId: Long,
    val brand: String,
    val modelName: String,
    val generationId: Long,
    val generationCode: String,
    val startYear: Int,
    val endYear: Int?,
    val trimId: Long,
    val trimName: String,
) {

    // 보유 차량 연식은 세대의 판매 기간 안에서만 고를 수 있다 (판매 중이면 올해 + 1 까지)
    fun coversYear(year: Int, currentYear: Int): Boolean {
        val lastYear = endYear ?: (currentYear + 1)
        return year >= startYear && year <= lastYear
    }
}
