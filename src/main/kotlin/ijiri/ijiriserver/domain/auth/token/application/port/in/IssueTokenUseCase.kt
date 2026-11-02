package ijiri.ijiriserver.domain.auth.token.application.port.`in`

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.domain.member.entity.Member

interface IssueTokenUseCase {

    fun issue(member: Member): AuthResponse
}
