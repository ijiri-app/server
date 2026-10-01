package ijiri.ijiriserver.domain.auth.email.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SignupRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 조건을_모두_지키면_통과한다() {
        assertThat(validator.validate(request("abcd1234", "이지리"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc1234", "abcdefgh", "12345678", "abcdefghij1234567890x"})
    void 비밀번호가_8_20자_영문_숫자_조건을_어기면_거부한다(String password) {
        assertThat(validator.validate(request(password, "이지리")))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"이", "열세글자닉네임입니다하나둘"})
    void 닉네임이_2_12자가_아니면_거부한다(String nickname) {
        assertThat(validator.validate(request("abcd1234", nickname)))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("nickname"));
    }

    private SignupRequest request(String password, String nickname) {
        return new SignupRequest(
                "user@ijiri.com",
                password,
                nickname,
                "vt_token",
                new SignupRequest.Agreements(true, true, true)
        );
    }
}
