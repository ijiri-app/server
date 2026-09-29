package ijiri.ijiriserver.domain.auth.token.service;

import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;
import ijiri.ijiriserver.domain.member.entity.Member;

public interface TokenService {

    TokenResponse issue(Member member);

    long getAccessTokenExpiresIn();

    TokenResponse refresh(String refreshToken);

    void logout(Long memberId, String refreshToken);
}
