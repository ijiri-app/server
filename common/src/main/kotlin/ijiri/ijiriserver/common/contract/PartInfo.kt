package ijiri.ijiriserver.common.contract

/**
 * service-car 의 부품 조회·등록 응답.
 */
data class PartInfo(
    val id: Long,
    val name: String,
    val category: PartCategory,
    val brandId: Long?,
    val brandName: String?,
)
