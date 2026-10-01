package ijiri.ijiriserver.domain.upload.controller;

import ijiri.ijiriserver.domain.upload.dto.response.UploadResponse;
import ijiri.ijiriserver.domain.upload.exception.UploadStatusCode;
import ijiri.ijiriserver.domain.upload.service.UploadService;
import ijiri.ijiriserver.global.exception.CommonStatusCode;
import ijiri.ijiriserver.global.exception.CustomException;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Upload", description = "사진 업로드")
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @Operation(
            summary = "사진 업로드",
            description = "multipart file 1장(최대 10MB, JPEG/PNG/WebP/HEIC). 위치 정보(EXIF GPS)는 앱에서 지우고 올린다. "
                    + "받은 imageId 를 게시물 작성에, url 을 프로필 수정에 쓴다. 하루 안에 쓰지 않으면 삭제된다"
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<UploadResponse> uploadImage(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @RequestPart("file") MultipartFile file
    ) {
        return BaseResponse.of(
                UploadStatusCode.UPLOAD_SUCCESS,
                uploadService.uploadImage(Long.valueOf(memberId), readBytes(file))
        );
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CustomException(CommonStatusCode.BAD_REQUEST);
        }
    }
}
