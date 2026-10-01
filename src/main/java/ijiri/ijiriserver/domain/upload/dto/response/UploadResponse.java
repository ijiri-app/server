package ijiri.ijiriserver.domain.upload.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import ijiri.ijiriserver.domain.upload.entity.UploadedImage;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UploadResponse(
        Long imageId,
        String url,
        Integer width,
        Integer height
) {

    public static UploadResponse image(UploadedImage image) {
        return new UploadResponse(image.getId(), image.getUrl(), image.getWidth(), image.getHeight());
    }
}
