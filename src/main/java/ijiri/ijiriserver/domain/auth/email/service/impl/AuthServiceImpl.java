package ijiri.ijiriserver.domain.auth.email.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.email.service.AuthService;
import ijiri.ijiriserver.domain.auth.email.service.EmailVerificationService;
import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.domain.member.dto.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    // 틀린 인증 시도 횟수가 가입 실패와 함께 롤백되지 않도록 전체를 하나의 트랜잭션으로 묶지 않는다.
    // 코드를 쓰기 전에 중복 이메일을 먼저 확인해, 가입이 불가능한 요청으로 코드가 소모되지 않게 한다
    @Override
    public AuthResponse signup(SignupRequest request) {
        if (memberService.existsEmailMember(request.email())) {
            throw new CustomException(MemberStatusCode.DUPLICATE_EMAIL);
        }
        emailVerificationService.verifyAndConsume(request.email(), request.verificationCode());
        Member member = memberService.signup(new MemberSignupCommand(
                request.email(),
                request.nickname(),
                passwordEncoder.encode(request.password())
        ));
        return AuthResponse.signup(MemberResponse.from(member));
    }

    @Override
    @Transactional
    public AuthResponse signIn(SignInRequest request) {
        // 이메일 존재 여부를 노출하지 않도록 "없는 이메일"과 "틀린 비밀번호"를 같은 에러로 응답한다
        Member member = memberService.findEmailMember(request.email())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_CREDENTIALS));
        return tokenService.issue(member).withSignIn(false, MemberResponse.from(member));
    }
}
