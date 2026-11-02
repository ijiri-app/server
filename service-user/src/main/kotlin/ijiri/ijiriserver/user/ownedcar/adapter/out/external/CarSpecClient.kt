package ijiri.ijiriserver.user.ownedcar.adapter.out.external

import ijiri.ijiriserver.common.contract.CarSpec
import ijiri.ijiriserver.common.internal.InternalClientFactory
import ijiri.ijiriserver.user.ownedcar.application.port.out.CarSpecReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CarSpecClient(
    factory: InternalClientFactory,
    @Value("\${services.car.url}") carUrl: String,
) : CarSpecReader {

    private val client = factory.forService(carUrl)

    override fun getSpecByTrim(trimId: Long): CarSpec = client.get("/internal/car-trims/{id}", trimId)

    override fun getSpecsByTrims(trimIds: Collection<Long>): Map<Long, CarSpec> {
        if (trimIds.isEmpty()) {
            return emptyMap()
        }
        return client.get<List<CarSpec>>("/internal/car-trims?ids={ids}", trimIds.joinToString(","))
            .associateBy { it.trimId }
    }
}
