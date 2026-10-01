package ijiri.ijiriserver.domain.upload.repository;

import ijiri.ijiriserver.domain.upload.entity.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {

    Optional<ImageFile> findByImageKey(String imageKey);

    boolean existsByImageKey(String imageKey);

    @Modifying
    @Query("DELETE FROM ImageFile f WHERE f.imageKey = :imageKey")
    void deleteByImageKey(@Param("imageKey") String imageKey);
}
