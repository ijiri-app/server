package ijiri.ijiriserver.user.interestcar.adapter.out.external

import ijiri.ijiriserver.common.contract.CarModelInfo
import ijiri.ijiriserver.common.internal.InternalClientFactory
import ijiri.ijiriserver.user.interestcar.application.port.out.CarModelReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CarModelClient(
    factory: InternalClientFactory,
    @Value("\${services.car.url}") carUrl: String,
) : CarModelReader {

    private val client = factory.forService(carUrl)

    override fun getCarModelInfos(carModelIds: Collection<Long>): List<CarModelInfo> {
        if (carModelIds.isEmpty()) {
            return listOf()
        }
        return client.get("/internal/car-models?ids={ids}", carModelIds.joinToString(","))
    }
}
