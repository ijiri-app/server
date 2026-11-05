package ijiri.ijiriserver.user.auth.email.application.service

import ijiri.ijiriserver.user.auth.common.application.data.response.AuthResponse
import ijiri.ijiriserver.user.auth.common.exception.AuthStatusCode
import ijiri.ijiriserver.user.auth.email.application.data.request.PasswordResetRequest
import ijiri.ijiriserver.user.auth.email.application.data.request.SignInRequest
import ijiri.ijiriserver.user.auth.email.application.data.request.SignupRequest
import ijiri.ijiriserver.user.auth.email.application.port.`in`.ConsumeResetCodeUseCase
import ijiri.ijiriserver.user.auth.email.application.port.`in`.ConsumeSignupTokenUseCase
import ijiri.ijiriserver.user.auth.email.application.port.`in`.EmailSignInUseCase
import ijiri.ijiriserver.user.auth.email.application.port.`in`.EmailSignupUseCase
import ijiri.ijiriserver.user.auth.email.application.port.`in`.ResetPasswordUseCase
import ijiri.ijiriserver.user.auth.email.application.port.`in`.SignOutUseCase
import ijiri.ijiriserver.user.auth.email.application.port.`in`.SignOutWithRefreshTokenUseCase
import ijiri.ijiriserver.user.auth.token.application.port.`in`.IssueTokenUseCase
import ijiri.ijiriserver.user.auth.token.application.port.`in`.RevokeMemberSessionsUseCase
import ijiri.ijiriserver.user.auth.token.application.port.`in`.RevokeRefreshTokenSessionUseCase
import ijiri.ijiriserver.user.member.application.data.MemberSignupCommand
import ijiri.ijiriserver.user.member.application.data.response.MemberResponse
import ijiri.ijiriserver.user.member.application.port.`in`.ChangeMemberPasswordUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.CreateEmailMemberUseCase
import ijiri.ijiriserver.user.member.application.port.`in`.FindEmailMemberUseCase
import ijiri.ijiriserver.user.member.exception.MemberStatusCode
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.ratelimit.RateLimiter
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration

@Service
class AuthService(
    private val changeMemberPasswordUseCase: ChangeMemberPasswordUseCase,
    private val findEmailMemberUseCase: FindEmailMemberUseCase,
    private val createEmailMemberUseCase: CreateEmailMemberUseCase,
    private val consumeResetCodeUseCase: ConsumeResetCodeUseCase,
    private val consumeSignupTokenUseCase: ConsumeSignupTokenUseCase,
    private val issueTokenUseCase: IssueTokenUseCase,
    private val revokeMemberSessionsUseCase: RevokeMemberSessionsUseCase,
    private val revokeRefreshTokenSessionUseCase: RevokeRefreshTokenSessionUseCase,
    private val passwordEncoder: PasswordEncoder,
    private val rateLimiter: RateLimiter,
    private val transactionTemplate: TransactionTemplate,
) : EmailSignupUseCase,
    EmailSignInUseCase,
    SignOutUseCase,
    SignOutWithRefreshTokenUseCase,
    ResetPasswordUseCase {

    // 인증 기록 소모, 회원 생성, 토큰 발급을 한 트랜잭션으로 묶어 중간에 실패하면 인증을 다시 쓸 수 있게 한다.
    // 가입 직후 바로 서비스를 쓰도록 로그인 응답과 같은 형태로 돌려준다
    @Transactional
    override fun signup(request: SignupRequest): AuthResponse {
        val email = request.email!!
        consumeSignupTokenUseCase.consumeSignupToken(email, request.verificationToken!!)
        val member = createEmailMemberUseCase.signup(
            MemberSignupCommand(
                email,
                request.nickname!!,
                passwordEncoder.encode(request.password!!),
            )
        )
        return issueTokenUseCase.issue(member).withSignIn(true, MemberResponse.summary(member))
    }

    // 이메일 존재 여부를 노출하지 않도록 "없는 이메일"과 "틀린 비밀번호"를 같은 에러로 응답하고,
    // 실패 횟수도 가입 여부와 무관하게 이메일별로 세어 잠근다
    override fun signIn(request: SignInRequest): AuthResponse {
        val email = request.email!!
        val failureKey = SIGNIN_FAILURE_KEY_PREFIX + email
        if (rateLimiter.isExhausted(failureKey, MAX_SIGNIN_FAILURES)) {
            throw CustomException(
                AuthStatusCode.ACCOUNT_LOCKED,
                mapOf("retryAfterSeconds" to rateLimiter.retryAfterSeconds(failureKey)),
            )
        }

        val found = findEmailMemberUseCase.findEmailMember(email)
        val passwordHash = found?.password ?: DUMMY_PASSWORD_HASH
        val matches = passwordEncoder.matches(request.password!!, passwordHash)
        if (found == null || !matches) {
            rateLimiter.tryAcquire(failureKey, MAX_SIGNIN_FAILURES, SIGNIN_LOCK_PERIOD)
            throw CustomException(AuthStatusCode.INVALID_CREDENTIALS)
        }
        rateLimiter.reset(failureKey)
        // 비밀번호가 맞은 뒤에만 탈퇴 여부를 알려, 탈퇴 계정 존재가 제3자에게 드러나지 않게 한다
        if (found.isWithdrawn()) {
            throw CustomException(MemberStatusCode.MEMBER_WITHDRAWN)
        }
        return issueTokenUseCase.issue(found).withSignIn(false, MemberResponse.summary(found))
    }

    // 모든 기기에서 로그아웃. 세션(refresh token)이 지워지므로 access token 도 즉시 거부된다
    override fun signOut(memberId: Long) {
        revokeMemberSessionsUseCase.signOut(memberId)
    }

    override fun signOutByRefreshToken(refreshToken: String) {
        revokeRefreshTokenSessionUseCase.signOutByRefreshToken(refreshToken)
    }

    // 코드 확인은 틀린 시도 횟수가 남도록 자기 트랜잭션에서 하고, 맞으면 비밀번호 변경과 세션 폐기를 한 트랜잭션으로 묶는다.
    // 비밀번호를 잊어 잠긴 계정도 재설정 후 바로 로그인할 수 있도록 실패 횟수를 지운다
    override fun resetPassword(request: PasswordResetRequest) {
        val email = request.email!!
        consumeResetCodeUseCase.consumeResetCode(email, request.code!!)
        val encoded = passwordEncoder.encode(request.newPassword!!)
        transactionTemplate.executeWithoutResult {
            revokeMemberSessionsUseCase.signOut(changeMemberPasswordUseCase.changePassword(email, encoded))
        }
        rateLimiter.reset(SIGNIN_FAILURE_KEY_PREFIX + email)
    }

    companion object {
        // 없는 이메일로 로그인해도 BCrypt 비교를 한 번 수행해 응답 시간으로 가입 여부가 드러나지 않게 한다
        private const val DUMMY_PASSWORD_HASH =
            "\$2a\$10\$ADUVieyM54fCzHFbn9mvoumLLcttlgqVdwaYJu9QihGyTw570u1.."
        // 계정(이메일)별 비밀번호 대입 방지: 첫 실패부터 15분 안에 5번 틀리면 남은 시간 동안 잠근다
        private const val SIGNIN_FAILURE_KEY_PREFIX = "signin:failure:"
        private const val MAX_SIGNIN_FAILURES = 5
        private val SIGNIN_LOCK_PERIOD = Duration.ofMinutes(15)
    }
}
