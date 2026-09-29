package ijiri.ijiriserver.domain.auth.token.service;

import ijiri.ijiriserver.domain.auth.token.dto.response.TokenResponse;
import ijiri.ijiriserver.domain.member.entity.Member;

/**
 * 로그인 성공 시 access/refresh token 발급
 */
public interface TokenIssueService {

    TokenResponse issue(Member member);
}
