package ijiri.ijiriserver.domain.auth.common.service.impl;

import ijiri.ijiriserver.domain.auth.common.dto.SocialUserInfo;
import ijiri.ijiriserver.domain.auth.common.dto.response.LoginResponse;
import ijiri.ijiriserver.domain.auth.common.service.SocialLoginService;
import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;
import ijiri.ijiriserver.domain.auth.token.service.TokenIssueService;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Role;
import ijiri.ijiriserver.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialLoginServiceImpl implements SocialLoginService {

    private final MemberRepository memberRepository;
    private final TokenIssueService tokenIssueService;

    @Override
    @Transactional
    public LoginResponse login(SocialUserInfo info) {
        Optional<Member> existing = memberRepository.findByProviderAndProviderUserId(info.provider(), info.providerUserId());
        boolean isNewUser = existing.isEmpty();
        Member member = existing.orElseGet(() -> memberRepository.save(Member.builder()
                .provider(info.provider())
                .providerUserId(info.providerUserId())
                .email(info.email())
                .nickname(info.nickname())
                .role(Role.USER)
                .build()));

        TokenResponse tokens = tokenIssueService.issue(member);
        return new LoginResponse(tokens.accessToken(), tokens.refreshToken(), isNewUser);
    }
}
