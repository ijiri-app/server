package ijiri.ijiriserver.domain.upload.application.port.out;

import ijiri.ijiriserver.domain.upload.entity.ImageFile;

import java.util.Optional;

public interface ImageFilePort {

    Optional<ImageFile> findByImageKey(String imageKey);

    void deleteByImageKey(String imageKey);

    ImageFile save(ImageFile entity);
}
