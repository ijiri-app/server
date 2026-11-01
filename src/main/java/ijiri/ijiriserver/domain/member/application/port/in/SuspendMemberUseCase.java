package ijiri.ijiriserver.domain.member.application.port.in;

import java.time.LocalDateTime;

public interface SuspendMemberUseCase {

    /**
     * until 까지 이용을 정지하고 세션을 끊는다. 이미 더 긴 정지가 걸려 있어도 until 로 덮어쓴다.
     * 이미 탈퇴한 회원이면 아무것도 하지 않는다 (신고 처리가 막히지 않도록).
     */
    void suspend(Long memberId, LocalDateTime until);
}
