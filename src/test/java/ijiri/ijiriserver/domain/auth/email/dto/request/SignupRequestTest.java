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
    void 영문_숫자_특수문자를_모두_포함한_8자_이상_비밀번호는_통과한다() {
        assertThat(validator.validate(request("abcd123!"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc12!", "abcdefg1", "abcdefg!", "1234567!", "abcd 1234"})
    void 조건을_하나라도_어기면_거부한다(String password) {
        assertThat(validator.validate(request(password)))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    private SignupRequest request(String password) {
        return new SignupRequest("닉네임", "user@ijiri.com", password, "A1B2C3");
    }
}
