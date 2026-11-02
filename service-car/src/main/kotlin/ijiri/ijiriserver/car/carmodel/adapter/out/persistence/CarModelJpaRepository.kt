package ijiri.ijiriserver.car.carmodel.adapter.out.persistence

import ijiri.ijiriserver.car.carmodel.entity.CarModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CarModelJpaRepository : JpaRepository<CarModel, Long> {

    // keyword 는 소문자, 빈 문자열이면 전체. coreOnly 가 true 면 핵심 계열만
    @Query(
        """
        SELECT m FROM CarModel m
        WHERE m.hidden = false
          AND (:coreOnly = false OR m.core = true)
          AND (LOWER(m.name) LIKE CONCAT('%', :keyword, '%') OR LOWER(m.brand) LIKE CONCAT('%', :keyword, '%'))
        ORDER BY m.core DESC, m.displayOrder ASC, m.id ASC
        """,
    )
    fun search(@Param("keyword") keyword: String, @Param("coreOnly") coreOnly: Boolean): List<CarModel>

    fun findByIdAndHiddenFalse(id: Long): CarModel?

    fun findByBrandAndName(brand: String, name: String): CarModel?

    fun countByIdInAndHiddenFalse(ids: Collection<Long>): Long
}
