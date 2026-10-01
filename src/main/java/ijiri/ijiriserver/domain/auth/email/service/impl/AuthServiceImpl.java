package ijiri.ijiriserver.domain.auth.email.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.PasswordResetRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import ijiri.ijiriserver.domain.auth.email.entity.VerificationPurpose;
import ijiri.ijiriserver.domain.auth.email.service.AuthService;
import ijiri.ijiriserver.domain.auth.email.service.EmailVerificationService;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.domain.member.dto.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // 없는 이메일로 로그인해도 BCrypt 비교를 한 번 수행해 응답 시간으로 가입 여부가 드러나지 않게 한다
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$ADUVieyM54fCzHFbn9mvoumLLcttlgqVdwaYJu9QihGyTw570u1..";
    // 계정(이메일)별 비밀번호 대입 방지: 첫 실패부터 15분 안에 5번 틀리면 남은 시간 동안 잠근다
    private static final String SIGNIN_FAILURE_KEY_PREFIX = "signin:failure:";
    private static final int MAX_SIGNIN_FAILURES = 5;
    private static final Duration SIGNIN_LOCK_PERIOD = Duration.ofMinutes(15);

    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;

    // 인증 기록 소모, 회원 생성, 토큰 발급을 한 트랜잭션으로 묶어 중간에 실패하면 인증을 다시 쓸 수 있게 한다.
    // 가입 직후 바로 서비스를 쓰도록 로그인 응답과 같은 형태로 돌려준다
    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        emailVerificationService.consumeVerified(
                request.email(),
                VerificationPurpose.SIGNUP,
                request.verificationToken()
        );
        Member member = memberService.signup(new MemberSignupCommand(
                request.email(),
                request.nickname(),
                passwordEncoder.encode(request.password())
        ));
        return tokenService.issue(member).withSignIn(true, MemberResponse.summary(member));
    }

    // 이메일 존재 여부를 노출하지 않도록 "없는 이메일"과 "틀린 비밀번호"를 같은 에러로 응답하고,
    // 실패 횟수도 가입 여부와 무관하게 이메일별로 세어 잠근다
    @Override
    public AuthResponse signIn(SignInRequest request) {
        String failureKey = SIGNIN_FAILURE_KEY_PREFIX + request.email();
        if (rateLimiter.isExhausted(failureKey, MAX_SIGNIN_FAILURES)) {
            throw new CustomException(
                    AuthStatusCode.ACCOUNT_LOCKED,
                    Map.of("retryAfterSeconds", rateLimiter.retryAfterSeconds(failureKey))
            );
        }

        Optional<Member> found = memberService.findEmailMember(request.email());
        String passwordHash = found.map(Member::getPassword).orElse(DUMMY_PASSWORD_HASH);
        boolean matches = passwordEncoder.matches(request.password(), passwordHash);
        if (found.isEmpty() || !matches) {
            rateLimiter.tryAcquire(failureKey, MAX_SIGNIN_FAILURES, SIGNIN_LOCK_PERIOD);
            throw new CustomException(AuthStatusCode.INVALID_CREDENTIALS);
        }
        rateLimiter.reset(failureKey);
        Member member = found.get();
        // 비밀번호가 맞은 뒤에만 탈퇴 여부를 알려, 탈퇴 계정 존재가 제3자에게 드러나지 않게 한다
        if (member.isWithdrawn()) {
            throw new CustomException(MemberStatusCode.MEMBER_WITHDRAWN);
        }
        return tokenService.issue(member).withSignIn(false, MemberResponse.summary(member));
    }

    // 모든 기기에서 로그아웃. 세션(refresh token)이 지워지므로 access token 도 즉시 거부된다
    @Override
    public void signOut(Long memberId) {
        tokenService.signOut(memberId);
    }

    @Override
    public void signOutByRefreshToken(String refreshToken) {
        tokenService.signOutByRefreshToken(refreshToken);
    }

    // 인증 기록 소모, 비밀번호 변경, 세션 폐기를 한 트랜잭션으로 묶는다.
    // 비밀번호를 잊어 잠긴 계정도 재설정 후 바로 로그인할 수 있도록 실패 횟수를 지운다
    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        emailVerificationService.consumeVerified(
                request.email(),
                VerificationPurpose.RESET_PASSWORD,
                request.verificationToken()
        );
        Long memberId = memberService.changePassword(request.email(), passwordEncoder.encode(request.newPassword()));
        tokenService.signOut(memberId);
        rateLimiter.reset(SIGNIN_FAILURE_KEY_PREFIX + request.email());
    }
}
