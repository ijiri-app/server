package ijiri.ijiriserver.user.member.application.port.`in`

import ijiri.ijiriserver.user.member.application.data.response.MemberResponse

interface GetMyProfileUseCase {

    fun getMe(memberId: Long): MemberResponse
}
