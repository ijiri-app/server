package ijiri.ijiriserver.domain.carmodel.application.port.in;

import ijiri.ijiriserver.domain.carmodel.entity.CarSpec;

public interface GetSpecByTrimCarModelUseCase {

    /**
     * 트림으로 모델·세대를 정한다. 없는 트림이면 INVALID_TRIM.
     */
    CarSpec getSpecByTrim(Long trimId);
}
