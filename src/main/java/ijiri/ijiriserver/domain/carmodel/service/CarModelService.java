package ijiri.ijiriserver.domain.carmodel.service;

import ijiri.ijiriserver.domain.carmodel.dto.CarModelInfo;
import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.dto.response.CarModelResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CarModelService {

    CarModelResponse getCarModels(String keyword, boolean coreOnly);

    CarModelResponse getCarModel(Long carModelId);

    /**
     * 모든 id 가 숨기지 않은 차종인지 확인한다. 하나라도 아니면 NOT_FOUND.
     */
    void validateCarModelsExist(Collection<Long> carModelIds);

    /**
     * 트림으로 모델·세대를 정한다. 없는 트림이면 INVALID_TRIM.
     */
    CarSpec getSpecByTrim(Long trimId);

    /**
     * 연식이 그 세대의 판매 기간 안인지 확인한다. 아니면 INVALID_MODEL_YEAR.
     */
    void validateModelYear(Long generationId, int year);

    /**
     * 요청 순서대로 돌려준다. 없는 id 는 빠진다.
     */
    List<CarModelInfo> getCarModelInfos(List<Long> carModelIds);

    Map<Long, String> getModelNames(Collection<Long> carModelIds);
}
