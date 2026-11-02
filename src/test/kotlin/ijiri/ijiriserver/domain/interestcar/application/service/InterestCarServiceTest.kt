package ijiri.ijiriserver.domain.interestcar.application.service

import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarModelInfosUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.ValidateCarModelsExistUseCase
import ijiri.ijiriserver.domain.interestcar.application.port.out.MemberInterestCarPort
import ijiri.ijiriserver.domain.interestcar.entity.MemberInterestCar
import ijiri.ijiriserver.domain.member.application.port.`in`.GetMemberUseCase
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class InterestCarServiceTest {

    private val repository: MemberInterestCarPort = mock()
    private val service = InterestCarService(
        repository,
        mock<GetMemberUseCase>(),
        mock<GetCarModelInfosUseCase>(),
        mock<ValidateCarModelsExistUseCase>(),
    )

    @Test
    fun 중복_차종이_있으면_아무것도_지우지_않고_거부한다() {
        assertThatThrownBy { service.replaceAll(1L, listOf(3L, 3L)) }.isInstanceOf(CustomException::class.java)
        verify(repository, never()).deleteAllByMemberIdInBulk(1L)
    }

    @Test
    fun 기존_목록을_지우고_요청_순서대로_저장한다() {
        service.replaceAll(1L, listOf(25L, 3L, 17L))

        verify(repository).deleteAllByMemberIdInBulk(1L)
        val saved = argumentCaptor<Iterable<MemberInterestCar>>()
        verify(repository).saveAll(saved.capture())
        assertThat(saved.firstValue.map { it.carModelId }).containsExactly(25L, 3L, 17L)
    }
}
