package ijiri.ijiriserver.common.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.nio.charset.StandardCharsets

class MaxUtf8BytesValidator : ConstraintValidator<MaxUtf8Bytes, String> {

    private var max: Int = 0

    override fun initialize(annotation: MaxUtf8Bytes) {
        max = annotation.value
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean =
        value == null || value.toByteArray(StandardCharsets.UTF_8).size <= max
}
