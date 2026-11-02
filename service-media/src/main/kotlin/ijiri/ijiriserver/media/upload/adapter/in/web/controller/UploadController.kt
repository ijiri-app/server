package ijiri.ijiriserver.media.upload.adapter.`in`.web.controller

import ijiri.ijiriserver.media.upload.application.data.request.UploadUrlRequest
import ijiri.ijiriserver.media.upload.application.data.response.UploadResponse
import ijiri.ijiriserver.media.upload.application.port.`in`.CreateUploadUrlsUseCase
import ijiri.ijiriserver.common.response.BaseResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Upload", description = "사진 업로드")
@RestController
class UploadController(
    private val createUploadUrlsUseCase: CreateUploadUrlsUseCase,
) {

    @Operation(
        summary = "업로드 URL 발급",
        description = "사진 수만큼 imageKey 와 uploadUrl(presigned PUT, 10분 유효)을 준다. 앱은 uploadUrl 로 " +
            "파일 본문을 PUT 하고(Content-Type 은 요청한 값, JPEG·WebP, 최대 10MB), imageKey 를 게시물 작성·" +
            "프로필 수정에 쓴다. 연결할 때 크기·형식을 확인하고, 하루 안에 연결하지 않은 사진은 지워진다",
    )
    @PostMapping("/uploads/images")
    fun createUploadUrls(
        @Parameter(hidden = true) @AuthenticationPrincipal memberId: String,
        @Valid @RequestBody request: UploadUrlRequest,
    ): BaseResponse<UploadResponse> {
        return BaseResponse.ok(createUploadUrlsUseCase.createUploadUrls(memberId.toLong(), request))
    }
}
