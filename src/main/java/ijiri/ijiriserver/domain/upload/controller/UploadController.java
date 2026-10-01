package ijiri.ijiriserver.domain.upload.controller;

import ijiri.ijiriserver.domain.upload.dto.request.UploadUrlRequest;
import ijiri.ijiriserver.domain.upload.dto.response.UploadResponse;
import ijiri.ijiriserver.domain.upload.service.UploadService;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Upload", description = "사진 업로드")
@RestController
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @Operation(
            summary = "업로드 URL 발급",
            description = "사진 수만큼 imageKey 와 uploadUrl(10분 유효)을 준다. 앱은 uploadUrl 로 파일 본문을 PUT 하고 "
                    + "(Content-Type 은 요청한 값, 최대 10MB), imageKey 를 게시물 작성·프로필 수정에 쓴다. "
                    + "10분 안에 연결하지 않은 사진은 지워진다"
    )
    @PostMapping("/uploads/images")
    public BaseResponse<UploadResponse> createUploadUrls(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            @Valid @RequestBody UploadUrlRequest request
    ) {
        return BaseResponse.ok(uploadService.createUploadUrls(Long.valueOf(memberId), request));
    }
}
