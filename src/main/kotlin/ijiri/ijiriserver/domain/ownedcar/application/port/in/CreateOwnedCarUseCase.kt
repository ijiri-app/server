package ijiri.ijiriserver.domain.ownedcar.application.port.`in`

import ijiri.ijiriserver.domain.ownedcar.application.data.request.OwnedCarCreateRequest
import ijiri.ijiriserver.domain.ownedcar.application.data.response.OwnedCarResponse

interface CreateOwnedCarUseCase {

    fun create(memberId: Long, request: OwnedCarCreateRequest): OwnedCarResponse
}
