package ijiri.ijiriserver.user.auth.token.application.port.`in`

import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.user.member.entity.Member

interface IssueTokenUseCase {

    fun issue(member: Member): AuthResponse
}
