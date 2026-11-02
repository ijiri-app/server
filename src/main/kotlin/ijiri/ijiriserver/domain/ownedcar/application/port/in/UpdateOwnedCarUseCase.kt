package ijiri.ijiriserver.domain.ownedcar.application.port.`in`

import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarUpdateRequest

interface UpdateOwnedCarUseCase {

    fun update(memberId: Long, ownedCarId: Long, request: OwnedCarUpdateRequest)
}
