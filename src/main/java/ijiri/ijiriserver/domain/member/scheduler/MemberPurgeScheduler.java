package ijiri.ijiriserver.domain.member.scheduler;

import ijiri.ijiriserver.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 탈퇴 후 보관 기간이 지난 회원을 완전히 삭제한다.
 * 회원마다 별도 트랜잭션으로 지워, 한 명의 실패(외부 저장소 오류 등)가 다른 회원 삭제를 막지 않게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPurgeScheduler {

    private final MemberService memberService;

    @Scheduled(cron = "0 20 4 * * *", zone = "Asia/Seoul")
    public void purgeWithdrawnMembers() {
        List<Long> memberIds = memberService.findPurgeTargetIds();
        int purged = 0;
        for (Long memberId : memberIds) {
            try {
                memberService.purge(memberId);
                purged++;
            } catch (RuntimeException e) {
                log.error("Member purge failed: memberId={}", memberId, e);
            }
        }
        log.info("Purged {}/{} withdrawn members", purged, memberIds.size());
    }
}
