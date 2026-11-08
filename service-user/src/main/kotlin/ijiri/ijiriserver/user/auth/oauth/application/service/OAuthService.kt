package ijiri.ijiriserver.user.auth.oauth.application.service

import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.user.auth.common.application.port.out.SocialTokenVerifier
import ijiri.ijiriserver.user.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.user.auth.oauth.application.data.request.OAuthSignInRequest
import ijiri.ijiriserver.user.auth.oauth.application.port.`in`.OAuthSignInUseCase
import ijiri.ijiriserver.user.auth.token.application.port.`in`.IssueTokenUseCase
import ijiri.ijiriserver.user.member.application.data.MemberRegisterCommand
import ijiri.ijiriserver.user.member.application.data.MemberRegisterResult
import ijiri.ijiriserver.user.member.application.data.response.MemberResponse
import ijiri.ijiriserver.user.member.application.port.`in`.RegisterSocialMemberUseCase
import ijiri.ijiriserver.user.member.entity.Provider
import ijiri.ijiriserver.common.exception.CustomException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class OAuthService(
    verifiers: List<SocialTokenVerifier>,
    private val registerSocialMemberUseCase: RegisterSocialMemberUseCase,
    private val issueTokenUseCase: IssueTokenUseCase,
) : OAuthSignInUseCase {

    private val verifiers: Map<Provider, SocialTokenVerifier> = verifiers.associateBy { it.provider() }

    // 소셜 서버 호출(verify)이 DB 트랜잭션을 붙잡지 않도록 트랜잭션은 가입/토큰 저장 단위로만 건다
    override fun signIn(request: OAuthSignInRequest): AuthResponse {
        val verifier = verifiers[request.provider!!]
            ?: throw CustomException(AuthStatusCode.UNSUPPORTED_PROVIDER)
        val result = register(verifier.verify(request.token!!))
        val member = result.member
        return issueTokenUseCase.issue(member).withSignIn(result.isNewMember, MemberResponse.summary(member))
    }

    // 같은 계정의 첫 로그인이 동시에 들어오면 한쪽이 유니크 제약에 걸린다.
    // 그 트랜잭션은 롤백됐으므로 다시 호출하면 먼저 가입된 회원을 기존 회원으로 조회한다
    private fun register(command: MemberRegisterCommand): MemberRegisterResult =
        try {
            registerSocialMemberUseCase.registerIfAbsent(command)
        } catch (e: DataIntegrityViolationException) {
            registerSocialMemberUseCase.registerIfAbsent(command)
        }
}
