package ijiri.ijiriserver.user.interestcar.application.port.`in`

interface ReplaceInterestCarsUseCase {

    fun replaceAll(memberId: Long, carModelIds: List<Long>)
}
