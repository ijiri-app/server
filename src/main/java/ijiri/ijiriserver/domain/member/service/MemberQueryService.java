package ijiri.ijiriserver.domain.member.service;

import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;

public interface MemberQueryService {

    MemberResponse getMember(Long memberId);
}
