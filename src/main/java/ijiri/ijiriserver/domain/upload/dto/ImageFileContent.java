package ijiri.ijiriserver.domain.upload.dto;

public record ImageFileContent(
        String contentType,
        byte[] content
) {
}
