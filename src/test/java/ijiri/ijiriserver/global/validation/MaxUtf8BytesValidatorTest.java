package ijiri.ijiriserver.global.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxUtf8BytesValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 글자_수가_아니라_UTF8_바이트_수로_검사한다() {
        assertThat(validator.validate(new Target("a".repeat(72)))).isEmpty();
        // 한글 25자 = 75바이트
        assertThat(validator.validate(new Target("가".repeat(25)))).hasSize(1);
    }

    @Test
    void null_은_통과시킨다() {
        assertThat(validator.validate(new Target(null))).isEmpty();
    }

    private record Target(@MaxUtf8Bytes(72) String value) {
    }
}
