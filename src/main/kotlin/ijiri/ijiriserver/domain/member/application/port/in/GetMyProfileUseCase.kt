package ijiri.ijiriserver.domain.member.application.port.`in`

import ijiri.ijiriserver.domain.member.application.data.response.MemberResponse

interface GetMyProfileUseCase {

    fun getMe(memberId: Long): MemberResponse
}
