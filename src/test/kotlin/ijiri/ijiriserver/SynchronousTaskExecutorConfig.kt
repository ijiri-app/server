package ijiri.ijiriserver

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor

/**
 * 인증 메일을 응답 뒤에 보내는 applicationTaskExecutor 를 같은 스레드에서 바로 실행하는 실행기로 바꾼다.
 * 통합 테스트가 요청 직후 메일 목(mock) 호출을 확인할 수 있게 한다.
 */
@TestConfiguration(proxyBeanMethods = false)
class SynchronousTaskExecutorConfig {

    @Bean
    fun applicationTaskExecutor(): TaskExecutor = SyncTaskExecutor()
}
