package ijiri.ijiriserver.community.block.application.port.`in`

import ijiri.ijiriserver.community.block.application.data.response.BlockResponse

interface GetBlockedMembersUseCase {

    fun getBlockedMembers(memberId: Long): BlockResponse
}
