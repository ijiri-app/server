package ijiri.ijiriserver.domain.member.application.port.in;

import ijiri.ijiriserver.domain.member.application.data.request.MemberUpdateRequest;
import ijiri.ijiriserver.domain.member.application.data.response.MemberResponse;

public interface UpdateMemberProfileUseCase {

    MemberResponse updateProfile(Long memberId, MemberUpdateRequest request);
}
