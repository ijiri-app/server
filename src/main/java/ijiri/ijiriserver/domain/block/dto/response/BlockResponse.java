package ijiri.ijiriserver.domain.block.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * block 도메인의 모든 API 응답. 차단 목록은 blockedMembers, 차단/해제는 message 만 채운다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BlockResponse(
        List<BlockedMember> blockedMembers,
        String message
) {

    public static BlockResponse list(List<BlockedMember> blockedMembers) {
        return new BlockResponse(blockedMembers, null);
    }

    public static BlockResponse message(String message) {
        return new BlockResponse(null, message);
    }

    public record BlockedMember(
            Long memberId,
            String nickname,
            String profileImageUrl
    ) {
    }
}
