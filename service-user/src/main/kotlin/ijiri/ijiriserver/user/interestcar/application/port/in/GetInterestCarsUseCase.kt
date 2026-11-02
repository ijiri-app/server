package ijiri.ijiriserver.user.interestcar.application.port.`in`

import ijiri.ijiriserver.user.interestcar.application.data.response.InterestCarResponse

interface GetInterestCarsUseCase {

    fun getAll(memberId: Long): InterestCarResponse
}
