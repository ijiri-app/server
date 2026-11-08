package ijiri.ijiriserver.car.carmodel.adapter.`in`.web.controller

import ijiri.ijiriserver.car.carmodel.application.port.`in`.GetCarModelInfosUseCase
import ijiri.ijiriserver.car.carmodel.application.port.`in`.GetCarSpecByTrimUseCase
import ijiri.ijiriserver.common.contract.CarModelInfo
import ijiri.ijiriserver.common.contract.CarSpec
import ijiri.ijiriserver.common.response.BaseResponse
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 다른 서비스(service-user 의 보유 차량·관심 차종, service-community 의 게시물)가 내부망에서 부른다.
 */
@Hidden
@RestController
@RequestMapping("/internal")
class CarModelInternalController(
    private val getCarSpecByTrimUseCase: GetCarSpecByTrimUseCase,
    private val getCarModelInfosUseCase: GetCarModelInfosUseCase,
) {

    @GetMapping("/car-trims/{trimId}")
    fun getSpec(@PathVariable trimId: Long): BaseResponse<CarSpec> =
        BaseResponse.ok(getCarSpecByTrimUseCase.getSpecByTrim(trimId))

    @GetMapping("/car-trims")
    fun getSpecs(@RequestParam(required = false) ids: List<Long>?): BaseResponse<List<CarSpec>> =
        BaseResponse.ok(getCarSpecByTrimUseCase.getSpecsByTrims(ids.orEmpty()))

    @GetMapping("/car-models")
    fun getCarModels(@RequestParam(required = false) ids: List<Long>?): BaseResponse<List<CarModelInfo>> =
        BaseResponse.ok(getCarModelInfosUseCase.getCarModelInfos(ids.orEmpty()))
}
