package ijiri.ijiriserver.domain.auth.oauth.service.impl;

import ijiri.ijiriserver.domain.auth.common.client.SocialTokenVerifier;
import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.oauth.dto.request.OAuthSignInRequest;
import ijiri.ijiriserver.domain.auth.oauth.service.OAuthService;
import ijiri.ijiriserver.domain.auth.token.service.TokenService;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OAuthServiceImpl implements OAuthService {

    private final Map<Provider, SocialTokenVerifier> verifiers;
    private final MemberService memberService;
    private final TokenService tokenService;

    public OAuthServiceImpl(
            List<SocialTokenVerifier> verifiers,
            MemberService memberService,
            TokenService tokenService
    ) {
        this.verifiers = verifiers.stream()
                .collect(Collectors.toMap(SocialTokenVerifier::provider, Function.identity()));
        this.memberService = memberService;
        this.tokenService = tokenService;
    }

    // 소셜 서버 호출(verify)이 DB 트랜잭션을 붙잡지 않도록 트랜잭션은 가입/토큰 저장 단위로만 건다
    @Override
    public AuthResponse signIn(OAuthSignInRequest request) {
        SocialTokenVerifier verifier = Optional.ofNullable(verifiers.get(request.provider()))
                .orElseThrow(() -> new CustomException(AuthStatusCode.UNSUPPORTED_PROVIDER));
        MemberRegisterResult result = register(verifier.verify(request.token()));
        Member member = result.member();
        return tokenService.issue(member).withSignIn(result.isNewMember(), MemberResponse.from(member));
    }

    // 같은 계정의 첫 로그인이 동시에 들어오면 한쪽이 유니크 제약에 걸린다.
    // 그 트랜잭션은 롤백됐으므로 다시 호출하면 먼저 가입된 회원을 기존 회원으로 조회한다
    private MemberRegisterResult register(MemberRegisterCommand command) {
        try {
            return memberService.registerIfAbsent(command);
        } catch (DataIntegrityViolationException e) {
            return memberService.registerIfAbsent(command);
        }
    }
}
