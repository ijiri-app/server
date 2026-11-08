package ijiri.ijiriserver.user.interestcar.adapter.out.persistence

import ijiri.ijiriserver.user.interestcar.entity.MemberInterestCar
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberInterestCarJpaRepository : JpaRepository<MemberInterestCar, Long> {

    fun findAllByMemberIdOrderByDisplayOrderAsc(memberId: Long): List<MemberInterestCar>

    // 전체 교체 시 INSERT 보다 먼저 즉시 실행되어야 unique 제약에 걸리지 않으므로 bulk delete 사용
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM MemberInterestCar c WHERE c.memberId = :memberId")
    fun deleteAllByMemberIdInBulk(@Param("memberId") memberId: Long)
}
