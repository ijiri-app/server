package ijiri.ijiriserver.common.transaction

import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * DB 롤백으로 되돌릴 수 없는 일(저장소 파일 삭제, 다른 서비스 호출)을 트랜잭션이 끝난 뒤로 미룬다.
 * 실패해도 이미 끝난 트랜잭션은 바꿀 수 없으므로 로그만 남긴다.
 */
object TransactionHooks {

    fun afterCommit(action: () -> Unit) {
        runOnCompletion(action, TransactionSynchronization.STATUS_COMMITTED)
    }

    fun afterRollback(action: () -> Unit) {
        runOnCompletion(action, TransactionSynchronization.STATUS_ROLLED_BACK)
    }

    private fun runOnCompletion(action: () -> Unit, expectedStatus: Int) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                if (status != expectedStatus) {
                    return
                }
                try {
                    action()
                } catch (e: RuntimeException) {
                    log.error("Action after transaction failed", e)
                }
            }
        })
    }

    private val log = LoggerFactory.getLogger(TransactionHooks::class.java)
}
