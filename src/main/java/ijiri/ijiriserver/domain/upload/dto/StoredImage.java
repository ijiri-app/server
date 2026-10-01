package ijiri.ijiriserver.domain.upload.dto;

public record StoredImage(
        String contentType,
        byte[] content
) {
}
