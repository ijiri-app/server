package ijiri.ijiriserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class IjiriServerApplication

fun main(args: Array<String>) {
    runApplication<IjiriServerApplication>(*args)
}
