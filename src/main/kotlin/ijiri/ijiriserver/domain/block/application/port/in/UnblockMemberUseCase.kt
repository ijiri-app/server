package ijiri.ijiriserver.domain.block.application.port.`in`

interface UnblockMemberUseCase {

    fun unblock(blockerId: Long, targetId: Long)
}
