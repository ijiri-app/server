package ijiri.ijiriserver.domain.auth.token.service;

/**
 * 회원의 모든 refresh token 폐기 (탈퇴 등)
 */
public interface TokenRevokeService {

    void revokeAll(Long memberId);
}
