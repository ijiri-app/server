package ijiri.ijiriserver.media.config

import ijiri.ijiriserver.common.storage.ImageUrlResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StorageConfig(
    @Value("\${storage.public-base-url}") private val publicBaseUrl: String,
) {

    @Bean
    fun imageUrlResolver(): ImageUrlResolver = ImageUrlResolver(publicBaseUrl)
}
