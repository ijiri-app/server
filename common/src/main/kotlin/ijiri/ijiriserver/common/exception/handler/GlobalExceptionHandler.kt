package ijiri.ijiriserver.common.exception.handler

import ijiri.ijiriserver.common.response.BaseResponse
import ijiri.ijiriserver.common.exception.CustomException
import ijiri.ijiriserver.common.exception.CommonStatusCode
import ijiri.ijiriserver.common.exception.StatusCode
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<BaseResponse<Map<String, Any?>>> =
        toResponse(e.statusCode, e.details)

    // @Valid @RequestBody 검증 실패 -> result.fields 에 칸별 사유
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
    ): ResponseEntity<BaseResponse<Map<String, Any?>>> {
        val errors = LinkedHashMap<String?, String?>()
        e.bindingResult.fieldErrors
            .forEach { errors.putIfAbsent(it.field, it.defaultMessage) }
        e.bindingResult.globalErrors
            .forEach { errors.putIfAbsent(it.objectName, it.defaultMessage) }
        return validationFailed(errors)
    }

    // @PathVariable, @RequestParam 검증 실패
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        e: ConstraintViolationException,
    ): ResponseEntity<BaseResponse<Map<String, Any?>>> {
        val errors = LinkedHashMap<String?, String?>()
        e.constraintViolations
            .forEach { errors.putIfAbsent(it.propertyPath.toString(), it.message) }
        return validationFailed(errors)
    }

    // Spring 6.1+ 메서드 파라미터 검증 실패 (@RequestBody 가 아닌 파라미터의 제약)
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(
        e: HandlerMethodValidationException,
    ): ResponseEntity<BaseResponse<Map<String, Any?>>> {
        val errors = LinkedHashMap<String?, String?>()
        e.parameterValidationResults.forEach { result ->
            errors.putIfAbsent(
                result.methodParameter.parameterName,
                result.resolvableErrors.firstOrNull()?.defaultMessage,
            )
        }
        return validationFailed(errors)
    }

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MissingServletRequestParameterException::class,
        MissingRequestHeaderException::class,
        MissingRequestCookieException::class,
        MethodArgumentTypeMismatchException::class,
    )
    fun handleBadRequest(e: Exception): ResponseEntity<BaseResponse<Void>> =
        toResponse(CommonStatusCode.BAD_REQUEST)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<BaseResponse<Void>> =
        toResponse(CommonStatusCode.METHOD_NOT_ALLOWED)

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(e: HttpMediaTypeNotSupportedException): ResponseEntity<BaseResponse<Void>> =
        toResponse(CommonStatusCode.UNSUPPORTED_MEDIA_TYPE)

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<BaseResponse<Void>> =
        toResponse(CommonStatusCode.NOT_FOUND)

    // 도메인에서 잡지 못한 unique 제약 충돌 (동시 요청 경합 등)
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(e: DataIntegrityViolationException): ResponseEntity<BaseResponse<Void>> {
        log.warn("Data integrity violation: {}", e.mostSpecificCause.message)
        return toResponse(CommonStatusCode.CONFLICT)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<BaseResponse<Void>> {
        log.error("Unhandled exception", e)
        return toResponse(CommonStatusCode.INTERNAL_SERVER_ERROR)
    }

    private fun validationFailed(fields: Map<String?, String?>): ResponseEntity<BaseResponse<Map<String, Any?>>> =
        toResponse(CommonStatusCode.VALIDATION_FAILED, mapOf("fields" to fields))

    private fun toResponse(statusCode: StatusCode): ResponseEntity<BaseResponse<Void>> =
        toResponse(statusCode, null)

    private fun <T> toResponse(statusCode: StatusCode, result: T?): ResponseEntity<BaseResponse<T>> =
        ResponseEntity.status(statusCode.httpStatus)
            .body(BaseResponse.onFailure(statusCode, result))

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
