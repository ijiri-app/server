package ijiri.ijiriserver.domain.auth.common.dto.response;

public record SignInResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        boolean isNewMember,
        SignInMemberResponse member
) {
}
