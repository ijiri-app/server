package ijiri.ijiriserver.domain.upload.client;

import ijiri.ijiriserver.domain.upload.exception.UploadStatusCode;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * storage.local.base-dir 에 저장하고, WebConfig 가 /images/** 로 서빙한다.
 */
@Slf4j
@Component
public class LocalImageStorageClient implements ImageStorageClient {

    private static final String URL_PATH = "/images/";

    private final Path baseDir;
    private final String publicBaseUrl;

    public LocalImageStorageClient(
            @Value("${storage.local.base-dir}") String baseDir,
            @Value("${storage.public-base-url}") String publicBaseUrl
    ) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String store(String key, byte[] content) {
        try {
            Files.createDirectories(baseDir);
            Files.write(resolve(key), content);
        } catch (IOException e) {
            log.error("Image store failed: key={}", key, e);
            throw new CustomException(UploadStatusCode.STORAGE_ERROR);
        }
        return publicBaseUrl + URL_PATH + key;
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.error("Image delete failed: key={}", key, e);
            throw new CustomException(UploadStatusCode.STORAGE_ERROR);
        }
    }

    // 키는 서버가 만든 UUID 지만, 저장 디렉터리 밖을 가리키지 못하도록 한 번 더 막는다
    private Path resolve(String key) {
        Path path = baseDir.resolve(key).normalize();
        if (!path.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
