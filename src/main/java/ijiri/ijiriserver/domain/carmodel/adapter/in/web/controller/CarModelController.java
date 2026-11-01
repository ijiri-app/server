package ijiri.ijiriserver.domain.carmodel.adapter.in.web.controller;

import ijiri.ijiriserver.domain.carmodel.application.data.response.CarModelResponse;
import ijiri.ijiriserver.domain.carmodel.application.port.in.GetCarModelUseCase;
import ijiri.ijiriserver.domain.carmodel.application.port.in.GetCarModelsUseCase;
import ijiri.ijiriserver.domain.carmodel.entity.CarModel;
import ijiri.ijiriserver.global.data.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "CarModel", description = "차종 마스터")
@RestController
@RequestMapping("/car-models")
@RequiredArgsConstructor
@SecurityRequirements
public class CarModelController {

    private final GetCarModelUseCase getCarModelUseCase;
    private final GetCarModelsUseCase getCarModelsUseCase;

    @Operation(
            summary = "차종(모델) 목록",
            description = "로그인 없이 관심 차종 고르기에 사용. q 는 차종명·브랜드 검색, core=true 면 핵심 계열만. "
                    + "핵심 계열이 먼저 온다"
    )
    @GetMapping
    public BaseResponse<CarModelResponse> getCarModels(
            @RequestParam(required = false) @Size(max = 50) String q,
            @RequestParam(defaultValue = "false") boolean core
    ) {
        return BaseResponse.ok(getCarModelsUseCase.getCarModels(q, core));
    }

    @Operation(
            summary = "차종 상세",
            description = "세대 / 트림. 보유 차량 등록에 사용"
    )
    @GetMapping("/{carModelId}")
    public BaseResponse<CarModelResponse> getCarModel(@PathVariable Long carModelId) {
        return BaseResponse.ok(getCarModelUseCase.getCarModel(carModelId));
    }
}
