package ijiri.ijiriserver.domain.upload.adapter.`in`.web.controller

import ijiri.ijiriserver.domain.upload.application.port.`in`.LoadImageFileUseCase
import ijiri.ijiriserver.domain.upload.application.port.`in`.ReceiveImageFileUseCase
import ijiri.ijiriserver.global.exception.exception.CustomException
import ijiri.ijiriserver.global.exception.status_code.CommonStatusCode
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.time.Duration

/**
 * DB 저장소(로컬·테스트) 전용: R2 를 대신해 presigned URL 업로드(PUT)와 공개 URL 조회(GET)를 받는다.
 * 저장소 자체를 흉내 내는 엔드포인트라 BaseResponse 로 감싸지 않는다. 운영(storage.type = r2)에서는 등록되지 않는다.
 */
@Hidden
@RestController
@ConditionalOnProperty(name = ["storage.type"], havingValue = "db", matchIfMissing = true)
class ImageFileController(
    private val loadImageFileUseCase: LoadImageFileUseCase,
    private val receiveImageFileUseCase: ReceiveImageFileUseCase,
) {

    @PutMapping("/uploads/files/{*imageKey}")
    fun upload(
        @PathVariable imageKey: String,
        @RequestParam expires: Long,
        @RequestParam signature: String,
        @RequestHeader(HttpHeaders.CONTENT_TYPE) contentType: String,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Void> {
        receiveImageFileUseCase.receive(
            stripLeadingSlash(imageKey),
            expires,
            signature,
            mimeType(contentType),
            readBody(httpRequest),
        )
        return ResponseEntity.ok().build()
    }

    @GetMapping("/images/{*imageKey}")
    fun getImage(@PathVariable imageKey: String): ResponseEntity<ByteArray> {
        val image = loadImageFileUseCase.load(stripLeadingSlash(imageKey))
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .cacheControl(IMAGE_CACHE)
            .body(image.content)
    }

    // 10MB 를 넘는 본문은 끝까지 읽지 않고 거부한다
    private fun readBody(httpRequest: HttpServletRequest): ByteArray {
        val content = try {
            httpRequest.inputStream.use { it.readNBytes(MAX_FILE_BYTES + 1) }
        } catch (e: IOException) {
            throw CustomException(CommonStatusCode.BAD_REQUEST)
        }
        if (content.size > MAX_FILE_BYTES) {
            throw CustomException(CommonStatusCode.PAYLOAD_TOO_LARGE)
        }
        if (content.isEmpty()) {
            throw CustomException(CommonStatusCode.BAD_REQUEST)
        }
        return content
    }

    // 클라이언트가 붙이는 charset 같은 파라미터는 빼고 image/jpeg 형태로만 비교한다
    private fun mimeType(contentType: String): String {
        try {
            val mediaType = MediaType.parseMediaType(contentType)
            return mediaType.type + "/" + mediaType.subtype
        } catch (e: InvalidMediaTypeException) {
            throw CustomException(CommonStatusCode.UNSUPPORTED_MEDIA_TYPE)
        }
    }

    // {*imageKey} 는 앞의 / 까지 담는다
    private fun stripLeadingSlash(imageKey: String): String {
        return imageKey.removePrefix("/")
    }

    companion object {
        private const val MAX_FILE_BYTES = 10 * 1024 * 1024

        // 키는 한 번 정해지면 내용이 바뀌지 않으므로 오래 캐시한다
        private val IMAGE_CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic()
    }
}
