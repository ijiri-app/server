package ijiri.ijiriserver.domain.auth.email.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.response.SignInMemberResponse;
import ijiri.ijiriserver.domain.auth.common.dto.response.SignInResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignInRequest;
import ijiri.ijiriserver.domain.auth.email.dto.request.SignupRequest;
import ijiri.ijiriserver.domain.auth.email.service.AuthService;
import ijiri.ijiriserver.domain.auth.email.service.EmailVerificationService;
import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.domain.member.dto.MemberSignupCommand;
import ijiri.ijiriserver.domain.member.entity.Member;
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

    @Override
    @Transactional
    public SignInResponse signup(SignupRequest request) {
        // 같은 트랜잭션이라 가입이 실패하면 인증 기록 소모도 롤백되어 다시 가입할 수 있다
        emailVerificationService.consumeVerified(request.email());
        Member member = memberService.signup(new MemberSignupCommand(
                request.email(),
                request.nickname(),
                passwordEncoder.encode(request.password())
        ));
        return toSignInResponse(member, true);
    }

    @Override
    @Transactional
    public SignInResponse signIn(SignInRequest request) {
        // 이메일 존재 여부를 노출하지 않도록 "없는 이메일"과 "틀린 비밀번호"를 같은 에러로 응답한다
        Member member = memberService.findEmailMember(request.email())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new CustomException(AuthStatusCode.INVALID_CREDENTIALS));
        return toSignInResponse(member, false);
    }

    private SignInResponse toSignInResponse(Member member, boolean isNewMember) {
        TokenResponse tokens = tokenService.issue(member);
        return new SignInResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokenService.getAccessTokenExpiresIn(),
                isNewMember,
                SignInMemberResponse.from(member)
        );
    }
}
