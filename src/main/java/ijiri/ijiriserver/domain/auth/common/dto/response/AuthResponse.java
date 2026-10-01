package ijiri.ijiriserver.domain.auth.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;

/**
 * auth 도메인의 모든 API 응답. 로그인·가입은 토큰 + isNewMember + member, 갱신은 토큰,
 * 인증 코드 확인은 verificationToken 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn,
        Boolean isNewMember,
        MemberResponse member,
        String verificationToken
) {

    public static AuthResponse tokens(String accessToken, String refreshToken, long accessTokenExpiresIn) {
        return new AuthResponse(accessToken, refreshToken, accessTokenExpiresIn, null, null, null);
    }

    public static AuthResponse verified(String verificationToken) {
        return new AuthResponse(null, null, null, null, null, verificationToken);
    }

    public AuthResponse withSignIn(boolean isNewMember, MemberResponse member) {
        return new AuthResponse(accessToken, refreshToken, accessTokenExpiresIn, isNewMember, member, null);
    }
}
