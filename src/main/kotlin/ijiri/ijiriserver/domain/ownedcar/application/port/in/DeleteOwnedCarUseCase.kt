package ijiri.ijiriserver.domain.ownedcar.application.port.`in`

interface DeleteOwnedCarUseCase {

    /**
     * 게시물이 연결된 차량은 지우지 않고 PAST 로 바꾼다.
     */
    fun delete(memberId: Long, ownedCarId: Long)
}
