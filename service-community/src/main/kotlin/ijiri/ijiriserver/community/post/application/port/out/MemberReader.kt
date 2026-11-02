package ijiri.ijiriserver.community.post.application.port.out

import ijiri.ijiriserver.common.contract.MemberSummary

/**
 * 회원은 service-user 에 있으므로 내부 API 로 읽는다. 탈퇴한 회원은 없는 것으로 본다.
 */
interface MemberReader {

    fun findActive(memberId: Long): MemberSummary?

    /**
     * 활성 회원만. 없는 id 는 빠진다.
     */
    fun getActive(memberIds: Collection<Long>): List<MemberSummary>
}
