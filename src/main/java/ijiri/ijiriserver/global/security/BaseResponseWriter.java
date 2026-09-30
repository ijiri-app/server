package ijiri.ijiriserver.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import ijiri.ijiriserver.global.exception.StatusCode;
import ijiri.ijiriserver.global.response.BaseResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security 필터 단계는 GlobalExceptionHandler 를 거치지 않으므로 BaseResponse 를 직접 써서 응답한다.
 */
@Component
@RequiredArgsConstructor
public class BaseResponseWriter {

    private final ObjectMapper objectMapper;

    public void writeFailure(HttpServletResponse response, StatusCode statusCode) throws IOException {
        response.setStatus(statusCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), BaseResponse.onFailure(statusCode));
    }
}
