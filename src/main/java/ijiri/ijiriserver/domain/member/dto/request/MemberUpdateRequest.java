package ijiri.ijiriserver.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 보낸 필드만 바꾼다 (null 이면 그대로).
 */
public record MemberUpdateRequest(
        @Schema(description = "닉네임 (2~12자, 중복 불가)", example = "새닉네임")
        @Size(min = 2, max = 12) String nickname,

        @Schema(description = "POST /uploads/images (purpose = PROFILE)로 올린 imageKey. 빈 문자열이면 사진 삭제")
        @Size(max = 100) String profileImageKey,

        @Schema(description = "상태 메시지 (50자 이하). 빈 문자열이면 삭제", example = "휠 고민 중")
        @Size(max = 50) String statusMessage
) {

    public MemberUpdateRequest {
        nickname = nickname == null ? null : nickname.strip();
        statusMessage = statusMessage == null ? null : statusMessage.strip();
    }
}
