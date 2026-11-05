package ijiri.ijiriserver.community.block.application.port.`in`

interface GetHiddenMemberIdsUseCase {

    /**
     * memberId 가 차단했거나 memberId 를 차단한 회원들. 서로의 게시물을 보여주지 않는 데 쓴다.
     */
    fun getHiddenMemberIds(memberId: Long): Set<Long>
}
