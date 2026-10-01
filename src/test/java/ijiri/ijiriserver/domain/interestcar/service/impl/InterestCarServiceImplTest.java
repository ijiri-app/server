package ijiri.ijiriserver.domain.interestcar.service.impl;

import ijiri.ijiriserver.domain.carmodel.service.CarModelService;
import ijiri.ijiriserver.domain.interestcar.dto.response.InterestCarResponse;
import ijiri.ijiriserver.domain.interestcar.repository.MemberInterestCarRepository;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterestCarServiceImplTest {

    private final MemberInterestCarRepository repository = mock(MemberInterestCarRepository.class);
    private final MemberService memberService = mock(MemberService.class);
    private final CarModelService carModelService = mock(CarModelService.class);
    private final InterestCarServiceImpl service = new InterestCarServiceImpl(
            repository,
            memberService,
            carModelService
    );

    @Test
    void 중복_차종이_있으면_아무것도_지우지_않고_거부한다() {
        assertThatThrownBy(() -> service.replaceAll(1L, List.of(3L, 3L))).isInstanceOf(CustomException.class);
        verify(repository, never()).deleteAllByMemberIdInBulk(1L);
    }

    @Test
    void 기존_목록을_지우고_요청_순서대로_저장한다() {
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        InterestCarResponse response = service.replaceAll(1L, List.of(25L, 3L, 17L));

        verify(repository).deleteAllByMemberIdInBulk(1L);
        assertThat(response.carModelIds()).containsExactly(25L, 3L, 17L);
    }
}
