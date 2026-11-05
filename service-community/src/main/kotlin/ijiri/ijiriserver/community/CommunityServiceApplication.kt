package ijiri.ijiriserver.community

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["ijiri.ijiriserver.common", "ijiri.ijiriserver.community"])
class CommunityServiceApplication

fun main(args: Array<String>) {
    runApplication<CommunityServiceApplication>(*args)
}
