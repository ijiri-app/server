package ijiri.ijiriserver.domain.auth.token.application.port.in;

import ijiri.ijiriserver.domain.auth.common.application.data.response.AuthResponse;
import ijiri.ijiriserver.domain.member.entity.Member;

public interface TokenUseCase {

    AuthResponse issue(Member member);

    AuthResponse refresh(String refreshToken);

    void signOut(Long memberId);

    void signOutByRefreshToken(String refreshToken);
}
