package ijiri.ijiriserver.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 보낸 필드만 바꾼다 (null 이면 그대로).
 */
public record MemberUpdateRequest(
        @Schema(description = "닉네임 (2~12자)", example = "이지리오너")
        @Size(min = 2, max = 12) String nickname,

        @Schema(description = "POST /uploads/images 로 올린 사진의 url. 빈 문자열이면 프로필 사진 삭제")
        @Size(max = 500) String profileImageUrl
) {

    public MemberUpdateRequest {
        nickname = nickname == null ? null : nickname.strip();
    }
}
