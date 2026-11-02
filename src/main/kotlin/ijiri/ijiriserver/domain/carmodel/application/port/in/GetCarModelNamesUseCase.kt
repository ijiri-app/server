package ijiri.ijiriserver.domain.carmodel.application.port.`in`

interface GetCarModelNamesUseCase {

    fun getModelNames(carModelIds: Collection<Long>): Map<Long, String>
}
