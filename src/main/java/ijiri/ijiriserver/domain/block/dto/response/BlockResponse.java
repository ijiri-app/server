package ijiri.ijiriserver.domain.block.dto.response;

import java.util.List;

/**
 * block 도메인의 모든 API 응답 (차단 목록). 차단·해제는 204 라 본문이 없다.
 */
public record BlockResponse(
        List<BlockedMember> items
) {

    public record BlockedMember(
            Long memberId,
            String nickname,
            String profileImageUrl
    ) {
    }
}
