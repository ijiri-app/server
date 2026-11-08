package ijiri.ijiriserver.user.member.application.port.`in`

import ijiri.ijiriserver.common.contract.MemberSummary

interface GetMemberSummaryUseCase {

    /**
     * 다른 서비스에 보여줄 활성 회원 정보. 없거나 탈퇴했으면 NOT_FOUND.
     */
    fun getSummary(memberId: Long): MemberSummary

    /**
     * 활성 회원만. 없는 id 는 빠진다.
     */
    fun getSummaries(memberIds: Collection<Long>): List<MemberSummary>
}
