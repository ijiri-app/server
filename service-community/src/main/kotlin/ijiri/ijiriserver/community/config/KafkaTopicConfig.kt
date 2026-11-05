package ijiri.ijiriserver.community.config

import ijiri.ijiriserver.common.messaging.Topics
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

/**
 * 이 서비스가 발행하는 토픽. 브로커에 없으면 기동할 때 만든다 (파티션 1개, 홈서버 브로커 1대).
 */
@Configuration
class KafkaTopicConfig {

    @Bean
    fun postDeletedTopic(): NewTopic = TopicBuilder.name(Topics.POST_DELETED).partitions(1).build()

    @Bean
    fun partUsageChangedTopic(): NewTopic = TopicBuilder.name(Topics.PART_USAGE_CHANGED).partitions(1).build()
}
