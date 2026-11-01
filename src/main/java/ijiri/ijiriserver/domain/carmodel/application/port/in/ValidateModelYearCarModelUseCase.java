package ijiri.ijiriserver.domain.carmodel.application.port.in;

public interface ValidateModelYearCarModelUseCase {

    /**
     * 연식이 그 세대의 판매 기간 안인지 확인한다. 아니면 INVALID_MODEL_YEAR.
     */
    void validateModelYear(Long generationId, int year);
}
