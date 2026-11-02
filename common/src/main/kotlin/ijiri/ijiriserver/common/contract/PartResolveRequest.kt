package ijiri.ijiriserver.common.contract

/**
 * POST /internal/parts/resolve. partId 가 있으면 기존 부품, 없으면 brandName(선택) + partName + category 로
 * 찾거나 확인 대기(PENDING) 부품으로 새로 만든다.
 */
data class PartResolveRequest(
    val partId: Long?,
    val brandName: String?,
    val partName: String?,
    val category: PartCategory,
)
