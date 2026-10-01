package ijiri.ijiriserver.domain.carmodel.service;

import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.dto.response.CarModelResponse;

import java.util.Collection;
import java.util.Map;

public interface CarModelService {

    CarModelResponse getCarModels();

    CarModelResponse getCarModel(Long carModelId);

    /**
     * 모든 id 가 존재하는 차종인지 확인한다. 하나라도 없으면 CARMODEL404.
     */
    void validateCarModelsExist(Collection<Long> carModelIds);

    /**
     * 세대가 모델에, 트림(선택)이 세대에 속하는지 확인하고 조합을 돌려준다. 맞지 않으면 CARMODEL4001.
     */
    CarSpec getSpec(Long carModelId, Long generationId, Long trimId);

    Map<Long, String> getModelNames(Collection<Long> carModelIds);
}
