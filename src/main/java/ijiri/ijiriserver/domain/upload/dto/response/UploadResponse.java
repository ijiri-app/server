package ijiri.ijiriserver.domain.upload.dto.response;

import java.util.List;

public record UploadResponse(
        List<Item> items
) {

    public record Item(
            String imageKey,
            String uploadUrl,
            long expiresIn
    ) {
    }
}
