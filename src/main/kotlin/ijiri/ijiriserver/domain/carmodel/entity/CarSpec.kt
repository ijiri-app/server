package ijiri.ijiriserver.domain.carmodel.entity

/**
 * 트림 하나로 정해지는 모델 / 세대 / 트림 조합.
 */
data class CarSpec(
    val carModelId: Long,
    val brand: String,
    val modelName: String,
    val generationId: Long,
    val generationCode: String,
    val trimId: Long,
    val trimName: String,
) {

    companion object {
        fun of(model: CarModel, generation: CarGeneration, trim: CarTrim): CarSpec {
            return CarSpec(
                model.id,
                model.brand,
                model.name,
                generation.id,
                generation.code,
                trim.id,
                trim.name,
            )
        }
    }
}
