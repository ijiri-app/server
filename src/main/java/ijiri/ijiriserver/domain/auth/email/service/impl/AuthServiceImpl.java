package ijiri.ijiriserver.domain.auth.email.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
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

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // 없는 이메일로 로그인해도 BCrypt 비교를 한 번 수행해 응답 시간으로 가입 여부가 드러나지 않게 한다
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$ADUVieyM54fCzHFbn9mvoumLLcttlgqVdwaYJu9QihGyTw570u1..";
    private static final String SIGNIN_KEY_PREFIX = "signin:email:";
    private static final int SIGNIN_LIMIT_PER_EMAIL = 10;
    private static final Duration SIGNIN_LIMIT_PERIOD = Duration.ofMinutes(15);

    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;

    // 인증 완료 기록을 먼저 소모해 이메일 소유가 증명된 뒤에만 가입하고, 중복 여부도 그때만 알려준다.
    // 가입된 이메일로는 인증 코드가 발송되지 않으므로 중복 에러는 동시 가입 경합에서만 난다.
    // 가입 직후 바로 서비스를 쓰도록(디자인: "구경하러 가기") 토큰까지 발급한다
    @Override
    public AuthResponse signup(SignupRequest request) {
        emailVerificationService.consumeVerified(request.email());
        Member member = memberService.signup(new MemberSignupCommand(
                request.email(),
                request.nickname(),
                passwordEncoder.encode(request.password())
        ));
        return tokenService.issue(member).withSignIn(true, MemberResponse.from(member));
    }

    // 이메일 존재 여부를 노출하지 않도록 "없는 이메일"과 "틀린 비밀번호"를 같은 에러로 응답한다.
    // 비밀번호 대입을 막기 위해 계정(이메일)별 시도 횟수를 제한한다
    @Override
    public AuthResponse signIn(SignInRequest request) {
        rateLimiter.check(SIGNIN_KEY_PREFIX + request.email(), SIGNIN_LIMIT_PER_EMAIL, SIGNIN_LIMIT_PERIOD);

        Optional<Member> found = memberService.findEmailMember(request.email());
        String passwordHash = found.map(Member::getPassword).orElse(DUMMY_PASSWORD_HASH);
        boolean matches = passwordEncoder.matches(request.password(), passwordHash);
        Member member = found.filter(ignored -> matches)
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_CREDENTIALS));
        // 비밀번호가 맞은 뒤에만 탈퇴 여부를 알려, 탈퇴 계정 존재가 제3자에게 드러나지 않게 한다
        if (member.isWithdrawn()) {
            throw new CustomException(MemberStatusCode.MEMBER_WITHDRAWN);
        }
        return tokenService.issue(member).withSignIn(false, MemberResponse.from(member));
    }

    // 모든 기기에서 로그아웃. 세션(refresh token)이 지워지므로 access token 도 즉시 거부된다
    @Override
    public AuthResponse signOut(Long memberId) {
        return tokenService.signOut(memberId);
    }

    @Override
    public AuthResponse signOutByRefreshToken(String refreshToken) {
        return tokenService.signOutByRefreshToken(refreshToken);
    }
}
