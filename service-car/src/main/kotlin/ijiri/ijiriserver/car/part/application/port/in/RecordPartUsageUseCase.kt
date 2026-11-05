package ijiri.ijiriserver.car.part.application.port.`in`

interface RecordPartUsageUseCase {

    /**
     * 게시물에 부품이 달리거나(delta = 1) 빠질 때(delta = -1) 전체·차종별 사용 수를 바꾼다.
     */
    fun recordUsage(partIds: Collection<Long>, carModelId: Long, delta: Int)
}
