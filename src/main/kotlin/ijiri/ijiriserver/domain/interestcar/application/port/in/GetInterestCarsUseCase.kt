package ijiri.ijiriserver.domain.interestcar.application.port.`in`

import ijiri.ijiriserver.domain.interestcar.application.data.response.InterestCarResponse

interface GetInterestCarsUseCase {

    fun getAll(memberId: Long): InterestCarResponse
}
