package ijiri.ijiriserver.car.part.application.port.out

interface PartCarModelUsagePort {

    fun add(partId: Long, carModelId: Long, delta: Int)
}
