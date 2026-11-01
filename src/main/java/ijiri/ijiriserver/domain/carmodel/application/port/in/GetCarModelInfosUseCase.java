package ijiri.ijiriserver.domain.carmodel.application.port.in;

import ijiri.ijiriserver.domain.carmodel.application.data.CarModelInfo;

import java.util.List;

public interface GetCarModelInfosUseCase {

    /**
     * 요청 순서대로 돌려준다. 없는 id 는 빠진다.
     */
    List<CarModelInfo> getCarModelInfos(List<Long> carModelIds);
}
