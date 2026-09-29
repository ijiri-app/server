package ijiri.ijiriserver.domain.auth.token.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
