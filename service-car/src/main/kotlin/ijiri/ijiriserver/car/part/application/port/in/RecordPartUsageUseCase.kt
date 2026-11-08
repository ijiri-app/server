package ijiri.ijiriserver.car.part.application.port.`in`

interface RecordPartUsageUseCase {

    /**
     * 게시물에 달린(added) 부품은 1 늘리고 빠진(removed) 부품은 1 줄여 전체·차종별 사용 수를 맞춘다.
     * messageId 가 있으면 이미 처리한 메시지는 건너뛴다 (중복 전달된 메시지를 두 번 세지 않는다).
     */
    fun recordUsage(messageId: String?, carModelId: Long, addedPartIds: Set<Long>, removedPartIds: Set<Long>)
}
