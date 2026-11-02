package ijiri.ijiriserver.community.block.application.port.`in`

interface DeleteMemberBlocksUseCase {

    /**
     * 회원이 한 차단과 받은 차단을 모두 지운다 (영구 삭제).
     */
    fun deleteAll(memberId: Long)
}
