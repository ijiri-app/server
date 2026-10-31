package ijiri.ijiriserver.domain.upload.application.port.out;

import ijiri.ijiriserver.domain.upload.entity.ImageFile;

import java.util.Optional;

public interface DbImageStoragePort {

    boolean isValidSignature(String imageKey, String contentType, long expires, String signature);

    void save(String imageKey, String contentType, byte[] content);

    Optional<ImageFile> load(String imageKey);
}
