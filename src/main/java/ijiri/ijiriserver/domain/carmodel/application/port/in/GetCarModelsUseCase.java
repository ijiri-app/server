package ijiri.ijiriserver.domain.carmodel.application.port.in;

import ijiri.ijiriserver.domain.carmodel.application.data.response.CarModelResponse;

public interface GetCarModelsUseCase {

    CarModelResponse getCarModels(String keyword, boolean coreOnly);
}
