package ijiri.ijiriserver.domain.auth.oauth.dto.response;

public record OAuthLoginResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        boolean isNewMember,
        OAuthMemberResponse member
) {
}
