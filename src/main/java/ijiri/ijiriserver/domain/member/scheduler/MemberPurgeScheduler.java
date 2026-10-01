package ijiri.ijiriserver.domain.member.scheduler;

import ijiri.ijiriserver.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 탈퇴 후 보관 기간이 지난 회원을 매일 새벽 4시에 완전히 삭제한다. 100명씩 끊어 읽고,
 * 회원마다 별도 트랜잭션으로 지워 한 명의 실패(외부 저장소 오류 등)가 다른 회원 삭제를 막지 않게 한다.
 * 회원 한 명 안에서는 위시리스트·태그 -> 게시물 -> 보유 차량 -> 회원 순서로 지운다 (리스너 @Order).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPurgeScheduler {

    private static final int BATCH_SIZE = 100;

    private final MemberService memberService;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void purgeWithdrawnMembers() {
        int purged = 0;
        int failed = 0;
        long afterId = 0;
        List<Long> memberIds = memberService.findPurgeTargetIds(afterId, BATCH_SIZE);
        while (!memberIds.isEmpty()) {
            for (Long memberId : memberIds) {
                try {
                    memberService.purge(memberId);
                    purged++;
                } catch (RuntimeException e) {
                    failed++;
                    log.error("Member purge failed: memberId={}", memberId, e);
                }
            }
            afterId = memberIds.getLast();
            memberIds = memberService.findPurgeTargetIds(afterId, BATCH_SIZE);
        }
        log.info("Purged {} withdrawn members ({} failed)", purged, failed);
    }
}
