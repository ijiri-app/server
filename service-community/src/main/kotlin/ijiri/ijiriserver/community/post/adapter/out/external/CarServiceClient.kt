package ijiri.ijiriserver.community.post.adapter.out.external

import ijiri.ijiriserver.common.contract.CarModelInfo
import ijiri.ijiriserver.common.contract.CarSpec
import ijiri.ijiriserver.common.contract.PartInfo
import ijiri.ijiriserver.common.contract.PartResolveRequest
import ijiri.ijiriserver.common.internal.InternalClientFactory
import ijiri.ijiriserver.community.post.application.port.out.CarReader
import ijiri.ijiriserver.community.post.application.port.out.PartReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * service-car 의 내부 API. 차종 마스터와 부품을 읽고 새 부품을 등록한다.
 */
@Component
class CarServiceClient(
    factory: InternalClientFactory,
    @Value("\${services.car.url}") carUrl: String,
) : CarReader,
    PartReader {

    private val client = factory.forService(carUrl)

    override fun getSpecByTrim(trimId: Long): CarSpec = client.get("/internal/car-trims/{id}", trimId)

    override fun getModelNames(carModelIds: Collection<Long>): Map<Long, String> {
        if (carModelIds.isEmpty()) {
            return emptyMap()
        }
        return client.get<List<CarModelInfo>>("/internal/car-models?ids={ids}", carModelIds.joinToString(","))
            .associate { it.id to it.name }
    }

    override fun resolve(request: PartResolveRequest): PartInfo = client.post("/internal/parts/resolve", request)

    override fun getPartInfos(partIds: Collection<Long>): Map<Long, PartInfo> {
        if (partIds.isEmpty()) {
            return emptyMap()
        }
        return client.get<List<PartInfo>>("/internal/parts?ids={ids}", partIds.joinToString(","))
            .associateBy { it.id }
    }
}
