package ijiri.ijiriserver.common.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * 문자열의 UTF-8 바이트 길이 상한. 글자 수가 아니라 바이트로 제한해야 하는 값(BCrypt 72바이트 등)에 쓴다.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [MaxUtf8BytesValidator::class])
annotation class MaxUtf8Bytes(
    val value: Int,
    val message: String = "최대 {value}바이트까지 입력할 수 있습니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
