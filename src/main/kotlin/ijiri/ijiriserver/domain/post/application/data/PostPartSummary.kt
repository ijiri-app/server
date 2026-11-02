package ijiri.ijiriserver.domain.post.application.data

import ijiri.ijiriserver.domain.part.entity.PartCategory

/**
 * 위시리스트 목록에 보여줄 게시물 부품 정보.
 */
data class PostPartSummary(
    val postPartId: Long,
    val postId: Long,
    val thumbnailUrl: String?,
    val category: PartCategory,
    val brandName: String?,
    val partName: String?,
    val carModelName: String?,
)
