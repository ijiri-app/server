package ijiri.ijiriserver.domain.member.application.data.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

/**
 * 보낸 필드만 바꾼다 (null 이면 그대로).
 */
class MemberUpdateRequest(
    nickname: String?,
    profileImageKey: String?,
    statusMessage: String?,
) {

    @Schema(description = "닉네임 (2~12자, 중복 불가)", example = "새닉네임")
    @field:Size(min = 2, max = 12)
    val nickname: String? = nickname?.trim()

    @Schema(description = "POST /uploads/images (purpose = PROFILE)로 받아 올린 imageKey. 빈 문자열이면 사진 삭제")
    @field:Size(max = 100)
    val profileImageKey: String? = profileImageKey

    @Schema(description = "상태 메시지 (50자 이하). 빈 문자열이면 삭제", example = "휠 고민 중")
    @field:Size(max = 50)
    val statusMessage: String? = statusMessage?.trim()
}
