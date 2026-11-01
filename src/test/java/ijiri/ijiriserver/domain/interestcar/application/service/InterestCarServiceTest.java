package ijiri.ijiriserver.domain.interestcar.application.service;

import ijiri.ijiriserver.domain.carmodel.application.port.in.GetCarModelInfosUseCase;
import ijiri.ijiriserver.domain.carmodel.application.port.in.ValidateCarModelsExistUseCase;
import ijiri.ijiriserver.domain.interestcar.application.port.out.MemberInterestCarPort;
import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar;
import ijiri.ijiriserver.domain.member.application.port.in.GetMemberUseCase;
import ijiri.ijiriserver.global.exception.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InterestCarServiceTest {

    private final MemberInterestCarPort repository = mock(MemberInterestCarPort.class);
    private final InterestCarService service = new InterestCarService(
            repository,
            mock(GetMemberUseCase.class),
            mock(GetCarModelInfosUseCase.class),
            mock(ValidateCarModelsExistUseCase.class)
    );

    @Test
    void 중복_차종이_있으면_아무것도_지우지_않고_거부한다() {
        assertThatThrownBy(() -> service.replaceAll(1L, List.of(3L, 3L))).isInstanceOf(CustomException.class);
        verify(repository, never()).deleteAllByMemberIdInBulk(1L);
    }

    @Test
    void 기존_목록을_지우고_요청_순서대로_저장한다() {
        service.replaceAll(1L, List.of(25L, 3L, 17L));

        verify(repository).deleteAllByMemberIdInBulk(1L);
        ArgumentCaptor<List<MemberInterestCar>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue())
                .extracting(MemberInterestCar::getCarModelId)
                .containsExactly(25L, 3L, 17L);
    }
}
