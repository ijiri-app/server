package ijiri.ijiriserver.user.auth.token.application.port.`in`

import ijiri.ijiriserver.user.auth.token.application.data.VerifiedMember

interface VerifyAccessTokenUseCase {

    /**
     * 서명·만료·세션(refresh token 행)·활성 회원을 모두 확인한다. 하나라도 아니면 null.
     */
    fun verify(accessToken: String): VerifiedMember?
}
