package ijiri.ijiriserver.user.ownedcar.application.port.`in`

import ijiri.ijiriserver.user.ownedcar.application.data.request.OwnedCarUpdateRequest

interface UpdateOwnedCarUseCase {

    fun update(memberId: Long, ownedCarId: Long, request: OwnedCarUpdateRequest)
}
