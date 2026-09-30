package ijiri.ijiriserver.domain.auth.token.service;

import ijiri.ijiriserver.domain.auth.common.dto.response.AuthResponse;
import ijiri.ijiriserver.domain.member.entity.Member;

public interface TokenService {

    AuthResponse issue(Member member);

    AuthResponse refresh(String refreshToken);

    AuthResponse signOut(Long memberId);
}
