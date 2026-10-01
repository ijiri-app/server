package ijiri.ijiriserver.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 로컬 디스크에 저장한 업로드 사진을 /images/** 로 서빙한다. 공용 저장소(S3 등)로 옮기면 지운다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String imageLocation;

    public WebConfig(@Value("${storage.local.base-dir}") String baseDir) {
        this.imageLocation = Path.of(baseDir).toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(imageLocation);
    }
}
