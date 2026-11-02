package ijiri.ijiriserver.domain.ownedcar.application.port.`in`

import ijiri.ijiriserver.domain.ownedcar.application.data.response.OwnedCarResponse

interface GetOwnedCarsUseCase {

    fun getAll(memberId: Long): OwnedCarResponse
}
