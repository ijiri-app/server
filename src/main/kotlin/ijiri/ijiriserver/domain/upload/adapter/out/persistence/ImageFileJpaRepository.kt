package ijiri.ijiriserver.domain.upload.adapter.out.persistence

import ijiri.ijiriserver.domain.upload.entity.ImageFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ImageFileJpaRepository : JpaRepository<ImageFile, Long> {

    fun findByImageKey(imageKey: String): ImageFile?

    @Modifying
    @Query("DELETE FROM ImageFile f WHERE f.imageKey = :imageKey")
    fun deleteByImageKey(@Param("imageKey") imageKey: String)
}
