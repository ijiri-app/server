package ijiri.ijiriserver.car

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["ijiri.ijiriserver.common", "ijiri.ijiriserver.car"])
class CarServiceApplication

fun main(args: Array<String>) {
    runApplication<CarServiceApplication>(*args)
}
