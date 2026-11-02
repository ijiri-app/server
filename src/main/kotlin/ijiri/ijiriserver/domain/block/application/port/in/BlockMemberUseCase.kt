package ijiri.ijiriserver.domain.block.application.port.`in`

interface BlockMemberUseCase {

    fun block(blockerId: Long, targetId: Long)
}
