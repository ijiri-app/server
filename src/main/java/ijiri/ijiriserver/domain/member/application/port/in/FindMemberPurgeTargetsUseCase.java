package ijiri.ijiriserver.domain.member.application.port.in;

import java.util.List;

public interface FindMemberPurgeTargetsUseCase {

    /**
     * 보관 기간이 지난 탈퇴 회원 id 를 afterId 다음부터 size 개.
     */
    List<Long> findPurgeTargetIds(Long afterId, int size);
}
