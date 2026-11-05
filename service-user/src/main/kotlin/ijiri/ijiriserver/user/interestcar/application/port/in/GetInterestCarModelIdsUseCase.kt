package ijiri.ijiriserver.user.interestcar.application.port.`in`

interface GetInterestCarModelIdsUseCase {

    /**
     * 피드 탭 순서대로. 없으면 빈 목록.
     */
    fun getCarModelIds(memberId: Long): List<Long>
}
