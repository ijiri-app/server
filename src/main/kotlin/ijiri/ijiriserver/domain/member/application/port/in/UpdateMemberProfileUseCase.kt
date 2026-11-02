package ijiri.ijiriserver.domain.member.application.port.`in`

import ijiri.ijiriserver.domain.member.application.data.request.MemberUpdateRequest
import ijiri.ijiriserver.domain.member.application.data.response.MemberResponse

interface UpdateMemberProfileUseCase {

    fun updateProfile(memberId: Long, request: MemberUpdateRequest): MemberResponse
}
