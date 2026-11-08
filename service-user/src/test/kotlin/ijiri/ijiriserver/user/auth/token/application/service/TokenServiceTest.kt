package ijiri.ijiriserver.user.auth.token.application.service

import ijiri.ijiriserver.user.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.user.auth.token.application.data.VerifiedMember
import ijiri.ijiriserver.user.auth.token.application.port.out.RefreshTokenPort
import ijiri.ijiriserver.user.member.application.port.`in`.FindActiveMemberUseCase
import ijiri.ijiriserver.user.member.entity.Member
import ijiri.ijiriserver.user.member.entity.Provider
import ijiri.ijiriserver.user.member.entity.Role
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.jwt.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TokenServiceTest {

    private val refreshTokenPort: RefreshTokenPort = mock()
    private val findActiveMemberUseCase: FindActiveMemberUseCase = mock()
    private val clock: Clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
    private val jwtProvider = JwtProvider(SECRET, 3600, 2419200, clock)
    private lateinit var tokenService: TokenService

    @BeforeEach
    fun setUp() {
        tokenService = TokenService(
            refreshTokenPort,
            findActiveMemberUseCase,
            jwtProvider,
            clock,
        )
    }

    @Test
    fun 발급하면_기존_토큰을_모두_지우고_새_토큰_하나만_저장한다() {
        tokenService.issue(
            Member(
                id = 7L,
                role = Role.USER,
                nickname = "이지리",
                provider = Provider.KAKAO,
                providerMemberId = "kakao-7",
            ),
        )

        inOrder(refreshTokenPort) {
            verify(refreshTokenPort).deleteAllByMemberId(7L)
            verify(refreshTokenPort).save(any())
        }
    }

    @Test
    fun 저장되지_않은_토큰은_거부하되_다른_기기의_토큰은_건드리지_않는다() {
        val replacedToken = jwtProvider.createRefreshToken("7", "session-1")
        whenever(refreshTokenPort.deleteValidByTokenHash(any(), any())).thenReturn(0)

        assertThatThrownBy { tokenService.refresh(replacedToken) }.isInstanceOf(CustomException::class.java)
        verify(refreshTokenPort, never()).deleteAllByMemberId(any())
        verify(findActiveMemberUseCase, never()).findActiveMember(any())
    }

    @Test
    fun 세션의_refresh_token_이_남아_있어야_access_token_이_유효하다() {
        whenever(refreshTokenPort.existsBySessionIdAndExpiresAtAfter(eq("session-1"), any())).thenReturn(true)

        assertThat(tokenService.isActive("session-1")).isTrue()
        assertThat(tokenService.isActive("deleted-session")).isFalse()
    }

    @Test
    fun 탈퇴한_회원의_refresh_token_은_404_가_아닌_유효하지_않은_토큰으로_거부한다() {
        val refreshToken = jwtProvider.createRefreshToken("7", "session-1")
        whenever(refreshTokenPort.deleteValidByTokenHash(any(), any())).thenReturn(1)
        whenever(findActiveMemberUseCase.findActiveMember(7L)).thenReturn(null)

        assertThatThrownBy { tokenService.refresh(refreshToken) }
            .isInstanceOfSatisfying(CustomException::class.java) { e ->
                assertThat(e.statusCode).isEqualTo(AuthStatusCode.INVALID_REFRESH_TOKEN)
            }
    }

    @Test
    fun 게이트웨이_검증은_세션과_활성_회원을_모두_확인하고_DB_의_역할을_준다() {
        val member = Member(
            id = 7L,
            role = Role.ADMIN,
            nickname = "이지리",
            provider = Provider.KAKAO,
            providerMemberId = "kakao-7",
        )
        val accessToken = jwtProvider.createAccessToken("7", "USER", "session-1")
        whenever(refreshTokenPort.existsBySessionIdAndExpiresAtAfter(eq("session-1"), any())).thenReturn(true)
        whenever(findActiveMemberUseCase.findActiveMember(7L)).thenReturn(member)

        val verified = tokenService.verify(accessToken)

        assertThat(verified).isEqualTo(VerifiedMember(7L, Role.ADMIN))
        assertThat(tokenService.verify(jwtProvider.createAccessToken("7", "USER", "deleted-session"))).isNull()
        assertThat(tokenService.verify(jwtProvider.createRefreshToken("7", "session-1"))).isNull()
    }

    @Test
    fun 로그아웃하면_회원의_refresh_token_을_모두_지운다() {
        tokenService.signOut(7L)

        verify(refreshTokenPort).deleteAllByMemberId(7L)
    }

    @Test
    fun access_token_으로는_갱신할_수_없다() {
        val accessToken = jwtProvider.createAccessToken("7", "USER", "session-1")

        assertThatThrownBy { tokenService.refresh(accessToken) }.isInstanceOf(CustomException::class.java)
        verify(refreshTokenPort, never()).deleteValidByTokenHash(any(), any())
    }

    companion object {
        private const val SECRET = "test-secret-key-0123456789abcdef-0123456789"
    }
}
