package ijiri.ijiriserver.user.ownedcar.application.port.`in`

import ijiri.ijiriserver.user.ownedcar.application.data.response.OwnedCarResponse

interface GetOwnedCarsUseCase {

    fun getAll(memberId: Long): OwnedCarResponse
}
