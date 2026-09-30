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

    // 틀린 인증 시도 횟수가 가입 실패와 함께 롤백되지 않도록 전체를 하나의 트랜잭션으로 묶지 않는다.
    // 코드 확인을 먼저 해 이메일 소유가 증명된 뒤에만 중복 여부를 알려준다.
    // 가입된 이메일로는 인증 코드가 발송되지 않으므로 중복 에러는 동시 가입 경합에서만 난다
    @Override
    public AuthResponse signup(SignupRequest request) {
        emailVerificationService.verifyAndConsume(request.email(), request.verificationCode());
        Member member = memberService.signup(new MemberSignupCommand(
                request.email(),
                request.nickname(),
                passwordEncoder.encode(request.password())
        ));
        return AuthResponse.signup(MemberResponse.from(member));
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
        return tokenService.issue(member).withSignIn(false, MemberResponse.from(member));
    }
}
