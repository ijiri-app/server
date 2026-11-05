package ijiri.ijiriserver.user.member.application.port.out

import ijiri.ijiriserver.common.contract.MemberCounts

/**
 * 마이 페이지·프로필의 게시물 수와 받은 담기 수. 게시물과 담기는 service-community 에 있으므로 내부 API 로 읽는다.
 */
interface MemberCountReader {

    fun countsOf(memberId: Long): MemberCounts
}
