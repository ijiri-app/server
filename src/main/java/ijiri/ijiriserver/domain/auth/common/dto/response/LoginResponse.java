package ijiri.ijiriserver.domain.auth.common.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
}
