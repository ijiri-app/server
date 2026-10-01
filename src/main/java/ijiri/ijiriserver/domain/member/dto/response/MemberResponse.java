package ijiri.ijiriserver.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;

/**
 * member 도메인의 모든 API 응답.
 * 로그인 응답의 member 는 summary, 내 정보는 me, 다른 회원 프로필은 profile(email·provider 제외).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        String statusMessage,
        Provider provider,
        String email,
        Long postCount,
        Long receivedWishCount
) {

    public static MemberResponse summary(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                null,
                null,
                null,
                null,
                null
        );
    }

    public static MemberResponse me(Member member, long postCount, long receivedWishCount) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getStatusMessage(),
                member.getProvider(),
                member.getEmail(),
                postCount,
                receivedWishCount
        );
    }

    public static MemberResponse profile(Member member, long postCount, long receivedWishCount) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getStatusMessage(),
                null,
                null,
                postCount,
                receivedWishCount
        );
    }
}
