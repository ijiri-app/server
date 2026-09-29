package ijiri.ijiriserver.domain.auth.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;

/**
 * auth 도메인의 모든 API 응답. API 마다 필요한 필드만 채우고 null 필드는 JSON 에서 빠진다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long accessTokenExpiresIn,
        Boolean isNewMember,
        MemberResponse member,
        Long verificationCodeExpiresIn,
        String message
) {

    public static AuthResponse tokens(String accessToken, String refreshToken, long accessTokenExpiresIn) {
        return new AuthResponse(accessToken, refreshToken, accessTokenExpiresIn, null, null, null, null);
    }

    public static AuthResponse signup(MemberResponse member) {
        return new AuthResponse(null, null, null, null, member, null, null);
    }

    public static AuthResponse verificationCodeSent(long verificationCodeExpiresIn) {
        return new AuthResponse(null, null, null, null, null, verificationCodeExpiresIn, null);
    }

    public static AuthResponse message(String message) {
        return new AuthResponse(null, null, null, null, null, null, message);
    }

    public AuthResponse withSignIn(boolean isNewMember, MemberResponse member) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                accessTokenExpiresIn,
                isNewMember,
                member,
                null,
                null
        );
    }
}
