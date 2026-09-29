package ijiri.ijiriserver.domain.auth.oauth.service.impl;

import ijiri.ijiriserver.domain.auth.common.client.SocialTokenVerifier;
import ijiri.ijiriserver.domain.auth.common.dto.SocialMemberInfo;
import ijiri.ijiriserver.domain.auth.common.exception.AuthStatusCode;
import ijiri.ijiriserver.domain.auth.oauth.dto.request.OAuthLoginRequest;
import ijiri.ijiriserver.domain.auth.oauth.dto.response.OAuthLoginResponse;
import ijiri.ijiriserver.domain.auth.oauth.dto.response.OAuthMemberResponse;
import ijiri.ijiriserver.domain.auth.oauth.service.OAuthService;
import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;
import ijiri.ijiriserver.domain.auth.token.service.TokenIssueService;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;
import ijiri.ijiriserver.domain.member.service.MemberRegisterService;
import ijiri.ijiriserver.global.exception.CustomException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OAuthServiceImpl implements OAuthService {

    private final Map<Provider, SocialTokenVerifier> verifiers;
    private final MemberRegisterService memberRegisterService;
    private final TokenIssueService tokenIssueService;

    public OAuthServiceImpl(
            List<SocialTokenVerifier> verifiers,
            MemberRegisterService memberRegisterService,
            TokenIssueService tokenIssueService
    ) {
        this.verifiers = verifiers.stream()
                .collect(Collectors.toMap(SocialTokenVerifier::provider, Function.identity()));
        this.memberRegisterService = memberRegisterService;
        this.tokenIssueService = tokenIssueService;
    }

    // 소셜 서버 호출(verify)이 DB 트랜잭션을 붙잡지 않도록 트랜잭션은 가입/토큰 저장 단위로만 건다
    @Override
    public OAuthLoginResponse login(OAuthLoginRequest request) {
        SocialMemberInfo info = resolveVerifier(request.provider()).verify(request.token());

        MemberRegisterResult result = memberRegisterService.registerIfAbsent(new MemberRegisterCommand(
                info.provider(),
                info.providerMemberId(),
                info.email(),
                info.username(),
                info.profileImageUrl()
        ));
        Member member = result.member();

        TokenResponse tokens = tokenIssueService.issue(member);
        return new OAuthLoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokenIssueService.getAccessTokenExpiresIn(),
                result.isNewMember(),
                OAuthMemberResponse.from(member)
        );
    }

    // enum 에 없는 값이거나 검증 구현체가 없는 provider 는 모두 UNSUPPORTED_PROVIDER
    private SocialTokenVerifier resolveVerifier(String provider) {
        return Arrays.stream(Provider.values())
                .filter(p -> p.name().equalsIgnoreCase(provider))
                .findFirst()
                .map(verifiers::get)
                .orElseThrow(() -> new CustomException(AuthStatusCode.UNSUPPORTED_PROVIDER));
    }
}
