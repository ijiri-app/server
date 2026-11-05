package ijiri.ijiriserver.user.ownedcar.application.port.`in`

import ijiri.ijiriserver.user.ownedcar.application.data.request.OwnedCarCreateRequest
import ijiri.ijiriserver.user.ownedcar.application.data.response.OwnedCarResponse

interface CreateOwnedCarUseCase {

    fun create(memberId: Long, request: OwnedCarCreateRequest): OwnedCarResponse
}
