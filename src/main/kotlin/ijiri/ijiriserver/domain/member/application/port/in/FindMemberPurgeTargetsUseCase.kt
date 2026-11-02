package ijiri.ijiriserver.domain.member.application.port.`in`

interface FindMemberPurgeTargetsUseCase {

    /**
     * 보관 기간이 지난 탈퇴 회원 id 를 afterId 다음부터 size 개.
     */
    fun findPurgeTargetIds(afterId: Long, size: Int): List<Long>
}
