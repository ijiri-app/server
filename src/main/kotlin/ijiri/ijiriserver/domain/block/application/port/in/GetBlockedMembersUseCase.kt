package ijiri.ijiriserver.domain.block.application.port.`in`

import ijiri.ijiriserver.domain.block.application.data.response.BlockResponse

interface GetBlockedMembersUseCase {

    fun getBlockedMembers(memberId: Long): BlockResponse
}
