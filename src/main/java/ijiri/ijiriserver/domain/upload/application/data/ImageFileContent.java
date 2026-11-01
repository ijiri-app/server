package ijiri.ijiriserver.domain.upload.application.data;

public record ImageFileContent(
        String contentType,
        byte[] content
) {
}
