package ijiri.ijiriserver.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 응답의 시각은 ISO 8601 에 한국 시간 오프셋을 붙여 내보낸다 (예: 2026-09-30T20:00:00+09:00).
 * 서버의 LocalDateTime 은 모두 Asia/Seoul 기준이다 (ClockConfig).
 */
@Configuration
public class JacksonConfig {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer offsetDateTimeCustomizer() {
        return builder -> builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider provider)
                    throws IOException {
                generator.writeString(value.truncatedTo(ChronoUnit.SECONDS)
                        .atZone(ZONE)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        });
    }
}
