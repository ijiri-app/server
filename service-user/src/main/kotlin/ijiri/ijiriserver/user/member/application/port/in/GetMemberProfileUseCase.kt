package ijiri.ijiriserver.user.member.application.port.`in`

import ijiri.ijiriserver.user.member.application.data.response.MemberResponse

interface GetMemberProfileUseCase {

    fun getProfile(memberId: Long): MemberResponse
}
