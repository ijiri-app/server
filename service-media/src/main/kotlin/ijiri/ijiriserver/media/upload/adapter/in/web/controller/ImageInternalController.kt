package ijiri.ijiriserver.media.upload.adapter.`in`.web.controller

import ijiri.ijiriserver.common.contract.AttachImagesRequest
import ijiri.ijiriserver.common.contract.AttachedImage
import ijiri.ijiriserver.common.contract.DeleteImagesRequest
import ijiri.ijiriserver.common.response.BaseResponse
import ijiri.ijiriserver.media.upload.application.port.`in`.AttachUploadedImagesUseCase
import ijiri.ijiriserver.media.upload.application.port.`in`.DeleteMemberImagesUseCase
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * service-community(게시물 사진)와 service-user(프로필 사진)가 내부망에서 부른다.
 */
@Hidden
@RestController
@RequestMapping("/internal/images")
class ImageInternalController(
    private val attachUploadedImagesUseCase: AttachUploadedImagesUseCase,
    private val deleteMemberImagesUseCase: DeleteMemberImagesUseCase,
) {

    @PostMapping("/attach")
    fun attach(@RequestBody request: AttachImagesRequest): BaseResponse<List<AttachedImage>> =
        BaseResponse.ok(attachUploadedImagesUseCase.attach(request.memberId, request.imageKeys, request.purpose))

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/delete")
    fun delete(@RequestBody request: DeleteImagesRequest) {
        deleteMemberImagesUseCase.delete(request.memberId, request.imageUrls, request.imageKeys)
    }
}
