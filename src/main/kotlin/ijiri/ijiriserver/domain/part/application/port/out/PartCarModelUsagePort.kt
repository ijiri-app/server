package ijiri.ijiriserver.domain.part.application.port.out

interface PartCarModelUsagePort {

    fun add(partId: Long, carModelId: Long, delta: Int)
}
