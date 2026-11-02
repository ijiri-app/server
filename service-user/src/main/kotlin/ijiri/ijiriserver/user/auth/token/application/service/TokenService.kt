package ijiri.ijiriserver.user.auth.token.application.service

import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.jwt.JwtProvider
import ijiri.ijiriserver.common.jwt.SessionValidator
import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.user.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.user.auth.token.application.data.VerifiedMember
import ijiri.ijiriserver.user.auth.token.application.port.`in`.IssueTokenUseCase
import ijiri.ijiriserver.user.auth.token.application.port.`in`.RefreshTokenUseCase
import ijiri.ijiriserver.user.auth.token.application.port.`in`.RevokeMemberSessionsUseCase
import ijiri.ijiriserver.user.auth.token.application.port.`in`.RevokeRefreshTokenSessionUseCase
import ijiri.ijiriserver.user.auth.token.application.port.`in`.VerifyAccessTokenUseCase
import ijiri.ijiriserver.user.auth.token.application.port.out.RefreshTokenPort
import ijiri.ijiriserver.user.auth.token.entity.RefreshToken
import ijiri.ijiriserver.user.member.application.port.`in`.FindActiveMemberUseCase
import ijiri.ijiriserver.user.member.entity.Member
import ijiri.ijiriserver.user.member.event.MemberSuspendedEvent
import ijiri.ijiriserver.user.member.event.MemberWithdrawnEvent
import ijiri.ijiriserver.user.member.exception.MemberStatusCode
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class TokenService(
    private val refreshTokenPort: RefreshTokenPort,
    private val findActiveMemberUseCase: FindActiveMemberUseCase,
    private val jwtProvider: JwtProvider,
    private val clock: Clock,
) : IssueTokenUseCase,
    RefreshTokenUseCase,
    RevokeMemberSessionsUseCase,
    RevokeRefreshTokenSessionUseCase,
    VerifyAccessTokenUseCase,
    SessionValidator {

    // 동시 접속 차단: 한 계정은 refresh token 을 하나만 가진다.
    // 새로 발급할 때 기존 토큰(세션)을 모두 지워, 다른 기기의 access token 도 즉시 거부된다.
    // 모든 로그인·갱신이 여기를 지나므로 이용 정지도 여기서 막는다
    @Transactional
    override fun issue(member: Member): AuthResponse {
        if (member.isSuspended(LocalDateTime.now(clock))) {
            throw CustomException(MemberStatusCode.MEMBER_SUSPENDED)
        }
        val subject = member.id.toString()
        val sessionId = UUID.randomUUID().toString()
        val accessToken = jwtProvider.createAccessToken(subject, member.role.name, sessionId)
        val refreshToken = jwtProvider.createRefreshToken(subject, sessionId)

        val expiresAt = LocalDateTime.now(clock).plusSeconds(jwtProvider.getRefreshTokenValiditySeconds())
        refreshTokenPort.deleteAllByMemberId(member.id)
        refreshTokenPort.save(RefreshToken.of(member.id, sessionId, refreshToken, expiresAt))
        return AuthResponse.tokens(accessToken, refreshToken, jwtProvider.getAccessTokenValiditySeconds())
    }

    // 서명이 유효해도 DB 에 없으면 교체(rotation), 로그아웃, 또는 다른 기기 로그인으로 밀려난 토큰이다.
    // 다른 기기 로그인으로 밀려난 경우와 구분할 수 없으므로, 회원의 토큰을 모두 폐기하지 않고 거부만 한다.
    // (그렇게 하면 밀려난 기기의 갱신 시도가 방금 로그인한 기기까지 로그아웃시킨다)
    // 탈취된 토큰이 쓰이면 원래 기기의 갱신이 실패하고, 다시 로그인하는 순간 탈취한 쪽 토큰이 폐기된다
    @Transactional
    override fun refresh(refreshToken: String): AuthResponse {
        val claims = jwtProvider.parseRefreshToken(refreshToken)
            ?: throw CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN)
        val deleted = refreshTokenPort.deleteValidByTokenHash(
            RefreshToken.hash(refreshToken),
            LocalDateTime.now(clock),
        )
        if (deleted == 0) {
            throw CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN)
        }
        // 탈퇴 등으로 활성 회원이 아니면 앱이 로그인 화면으로 가도록 404 가 아닌 401 로 응답한다
        val member = findActiveMemberUseCase.findActiveMember(claims.subject.toLong())
            ?: throw CustomException(AuthStatusCode.INVALID_REFRESH_TOKEN)
        return issue(member)
    }

    // 모든 기기에서 로그아웃. 세션(refresh token)이 지워지므로 access token 도 즉시 거부된다
    @Transactional
    override fun signOut(memberId: Long) {
        refreshTokenPort.deleteAllByMemberId(memberId)
    }

    // access token 이 만료돼도 로그아웃할 수 있게 refresh token 으로 세션을 지운다.
    // 저장된 토큰과 해시가 같아야만 지워지므로 서명·만료 검사는 필요 없고, 없는 토큰이어도 같은 응답을 준다
    @Transactional
    override fun signOutByRefreshToken(refreshToken: String) {
        refreshTokenPort.deleteByTokenHash(RefreshToken.hash(refreshToken))
    }

    // 게이트웨이가 모든 요청마다 부른다. 로그아웃·다른 기기 로그인·탈퇴로 세션이 끝났으면 만료 전 access token 도 거부하고,
    // 역할은 토큰의 클레임이 아니라 DB 의 현재 값을 준다
    @Transactional(readOnly = true)
    override fun verify(accessToken: String): VerifiedMember? {
        val claims = jwtProvider.parseAccessToken(accessToken) ?: return null
        val sessionId = jwtProvider.getSessionId(claims) ?: return null
        if (!isActive(sessionId)) {
            return null
        }
        val member = findActiveMemberUseCase.findActiveMember(claims.subject.toLong()) ?: return null
        return VerifiedMember(member.id, member.role)
    }

    @EventListener
    @Transactional
    fun revokeAll(event: MemberWithdrawnEvent) {
        refreshTokenPort.deleteAllByMemberId(event.memberId)
    }

    @EventListener
    @Transactional
    fun revokeAll(event: MemberSuspendedEvent) {
        refreshTokenPort.deleteAllByMemberId(event.memberId)
    }

    // session_id 유니크 인덱스 조회 1회
    @Transactional(readOnly = true)
    override fun isActive(sessionId: String): Boolean =
        refreshTokenPort.existsBySessionIdAndExpiresAtAfter(sessionId, LocalDateTime.now(clock))
}
