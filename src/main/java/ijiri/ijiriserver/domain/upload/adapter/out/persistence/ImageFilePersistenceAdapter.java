package ijiri.ijiriserver.domain.upload.adapter.out.persistence;

import ijiri.ijiriserver.domain.upload.application.port.out.ImageFilePort;
import ijiri.ijiriserver.domain.upload.entity.ImageFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ImageFilePersistenceAdapter implements ImageFilePort {

    private final ImageFileJpaRepository imageFileJpaRepository;

    @Override
    public Optional<ImageFile> findByImageKey(String imageKey) {
        return imageFileJpaRepository.findByImageKey(imageKey);
    }

    @Override
    public void deleteByImageKey(String imageKey) {
        imageFileJpaRepository.deleteByImageKey(imageKey);
    }

    @Override
    public ImageFile save(ImageFile entity) {
        return imageFileJpaRepository.save(entity);
    }
}
