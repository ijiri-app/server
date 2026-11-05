package ijiri.ijiriserver.user.ownedcar.adapter.out.persistence

import ijiri.ijiriserver.user.ownedcar.entity.OwnedCar
import ijiri.ijiriserver.user.ownedcar.entity.OwnedCarStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OwnedCarJpaRepository : JpaRepository<OwnedCar, Long> {

    fun findAllByMemberIdOrderByIdAsc(memberId: Long): List<OwnedCar>

    fun findByIdAndMemberId(id: Long, memberId: Long): OwnedCar?

    fun countByMemberIdAndStatus(memberId: Long, status: OwnedCarStatus): Long

    @Modifying
    @Query("DELETE FROM OwnedCar c WHERE c.memberId = :memberId")
    fun deleteAllByMemberIdInBulk(@Param("memberId") memberId: Long)
}
