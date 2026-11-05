package ijiri.ijiriserver.car.part.adapter.`in`.web.controller

import ijiri.ijiriserver.car.part.application.port.`in`.GetPartInfosUseCase
import ijiri.ijiriserver.car.part.application.port.`in`.ResolvePartUseCase
import ijiri.ijiriserver.common.contract.PartInfo
import ijiri.ijiriserver.common.contract.PartResolveRequest
import ijiri.ijiriserver.common.response.BaseResponse
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * service-community 가 게시물을 올리고 보여줄 때 내부망에서 부른다.
 */
@Hidden
@RestController
@RequestMapping("/internal/parts")
class PartInternalController(
    private val resolvePartUseCase: ResolvePartUseCase,
    private val getPartInfosUseCase: GetPartInfosUseCase,
) {

    @PostMapping("/resolve")
    fun resolve(@RequestBody request: PartResolveRequest): BaseResponse<PartInfo> =
        BaseResponse.ok(resolvePartUseCase.resolve(request))

    @GetMapping
    fun getParts(@RequestParam(required = false) ids: List<Long>?): BaseResponse<List<PartInfo>> =
        BaseResponse.ok(getPartInfosUseCase.getPartInfos(ids.orEmpty()).values.toList())
}
