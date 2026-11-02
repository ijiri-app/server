package ijiri.ijiriserver.domain.auth.email.application.data.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SignupRequestTest {

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun 조건을_모두_지키면_통과한다() {
        assertThat(validator.validate(request("abcd1234", "이지리"))).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["abc1234", "abcdefgh", "12345678", TOO_LONG_PASSWORD])
    fun 비밀번호가_8_64자_영문_숫자_조건을_어기면_거부한다(password: String) {
        assertThat(validator.validate(request(password, "이지리")))
            .anyMatch { violation -> violation.propertyPath.toString() == "password" }
    }

    @ParameterizedTest
    @ValueSource(strings = ["이", "열세글자닉네임입니다하나둘"])
    fun 닉네임이_2_12자가_아니면_거부한다(nickname: String) {
        assertThat(validator.validate(request("abcd1234", nickname)))
            .anyMatch { violation -> violation.propertyPath.toString() == "nickname" }
    }

    private fun request(password: String, nickname: String): SignupRequest =
        SignupRequest(
            "user@ijiri.com",
            password,
            nickname,
            "vt_token",
            SignupRequest.Agreements(true, true, true),
        )

    companion object {
        // 65자
        private const val TOO_LONG_PASSWORD =
            "a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1x"
    }
}
