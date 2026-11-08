package ijiri.ijiriserver.common.messaging

/**
 * 게시물에 부품이 달리거나 빠졌다. service-car 가 전체·차종별 사용 수를 맞춘다.
 */
data class PartUsageChangedMessage(
    val carModelId: Long,
    val addedPartIds: Set<Long>,
    val removedPartIds: Set<Long>,
)
