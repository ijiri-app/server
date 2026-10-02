package ijiri.ijiriserver.domain.upload.controller;

import ijiri.ijiriserver.domain.upload.dto.ImageFileContent;
import ijiri.ijiriserver.domain.upload.service.DbImageFileService;
import ijiri.ijiriserver.global.exception.CommonStatusCode;
import ijiri.ijiriserver.global.exception.CustomException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * DB 저장소(로컬·테스트) 전용: R2 를 대신해 presigned URL 업로드(PUT)와 공개 URL 조회(GET)를 받는다.
 * 저장소 자체를 흉내 내는 엔드포인트라 BaseResponse 로 감싸지 않는다. 운영(storage.type = r2)에서는 등록되지 않는다.
 */
@Hidden
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "db", matchIfMissing = true)
public class ImageFileController {

    private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    // 키는 한 번 정해지면 내용이 바뀌지 않으므로 오래 캐시한다
    private static final CacheControl IMAGE_CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic();

    private final DbImageFileService dbImageFileService;

    @PutMapping("/uploads/files/{*imageKey}")
    public ResponseEntity<Void> upload(
            @PathVariable String imageKey,
            @RequestParam long expires,
            @RequestParam String signature,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
            HttpServletRequest httpRequest
    ) {
        dbImageFileService.receive(
                stripLeadingSlash(imageKey),
                expires,
                signature,
                mimeType(contentType),
                readBody(httpRequest)
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/images/{*imageKey}")
    public ResponseEntity<byte[]> getImage(@PathVariable String imageKey) {
        ImageFileContent image = dbImageFileService.load(stripLeadingSlash(imageKey));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(IMAGE_CACHE)
                .body(image.content());
    }

    // 10MB 를 넘는 본문은 끝까지 읽지 않고 거부한다
    private byte[] readBody(HttpServletRequest httpRequest) {
        try (InputStream input = httpRequest.getInputStream()) {
            byte[] content = input.readNBytes(MAX_FILE_BYTES + 1);
            if (content.length > MAX_FILE_BYTES) {
                throw new CustomException(CommonStatusCode.PAYLOAD_TOO_LARGE);
            }
            if (content.length == 0) {
                throw new CustomException(CommonStatusCode.BAD_REQUEST);
            }
            return content;
        } catch (IOException e) {
            throw new CustomException(CommonStatusCode.BAD_REQUEST);
        }
    }

    // 클라이언트가 붙이는 charset 같은 파라미터는 빼고 image/jpeg 형태로만 비교한다
    private String mimeType(String contentType) {
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return mediaType.getType() + "/" + mediaType.getSubtype();
        } catch (InvalidMediaTypeException e) {
            throw new CustomException(CommonStatusCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    // {*imageKey} 는 앞의 / 까지 담는다
    private String stripLeadingSlash(String imageKey) {
        return imageKey.startsWith("/") ? imageKey.substring(1) : imageKey;
    }
}
