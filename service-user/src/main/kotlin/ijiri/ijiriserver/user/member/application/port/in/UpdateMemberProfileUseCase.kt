package ijiri.ijiriserver.user.member.application.port.`in`

import ijiri.ijiriserver.user.member.application.data.request.MemberUpdateRequest
import ijiri.ijiriserver.user.member.application.data.response.MemberResponse

interface UpdateMemberProfileUseCase {

    fun updateProfile(memberId: Long, request: MemberUpdateRequest): MemberResponse
}
