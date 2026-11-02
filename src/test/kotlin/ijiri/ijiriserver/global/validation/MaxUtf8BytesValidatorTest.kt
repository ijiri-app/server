package ijiri.ijiriserver.global.validation

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MaxUtf8BytesValidatorTest {

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun 글자_수가_아니라_UTF8_바이트_수로_검사한다() {
        assertThat(validator.validate(Target("a".repeat(72)))).isEmpty()
        // 한글 25자 = 75바이트
        assertThat(validator.validate(Target("가".repeat(25)))).hasSize(1)
    }

    @Test
    fun null_은_통과시킨다() {
        assertThat(validator.validate(Target(null))).isEmpty()
    }

    private data class Target(@field:MaxUtf8Bytes(72) val value: String?)
}
