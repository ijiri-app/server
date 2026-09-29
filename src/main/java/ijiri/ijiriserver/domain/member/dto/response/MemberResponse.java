package ijiri.ijiriserver.domain.member.dto.response;

import ijiri.ijiriserver.domain.member.entity.Member;
import ijiri.ijiriserver.domain.member.entity.Provider;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        Provider provider,
        String email,
        String username,
        LocalDateTime createdAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getProvider(),
                member.getEmail(),
                member.getUsername(),
                member.getCreatedAt());
    }
}
