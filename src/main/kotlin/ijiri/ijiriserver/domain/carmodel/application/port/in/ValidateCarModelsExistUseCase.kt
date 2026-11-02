package ijiri.ijiriserver.domain.carmodel.application.port.`in`

interface ValidateCarModelsExistUseCase {

    /**
     * 모든 id 가 숨기지 않은 차종인지 확인한다. 하나라도 아니면 NOT_FOUND.
     */
    fun validateCarModelsExist(carModelIds: Collection<Long>)
}
