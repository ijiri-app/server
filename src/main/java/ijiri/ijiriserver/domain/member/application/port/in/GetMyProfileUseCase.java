package ijiri.ijiriserver.domain.member.application.port.in;

import ijiri.ijiriserver.domain.member.application.data.response.MemberResponse;

public interface GetMyProfileUseCase {

    MemberResponse getMe(Long memberId);
}
