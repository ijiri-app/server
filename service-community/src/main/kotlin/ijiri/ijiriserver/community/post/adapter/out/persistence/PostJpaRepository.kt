package ijiri.ijiriserver.community.post.adapter.out.persistence

import ijiri.ijiriserver.community.post.entity.Post
import ijiri.ijiriserver.community.post.entity.PostStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostJpaRepository : JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    fun findAllByMemberId(memberId: Long): List<Post>

    fun countByMemberIdAndStatus(memberId: Long, status: PostStatus): Long

    @Query("SELECT p.ownedCarId, COUNT(p) FROM Post p WHERE p.ownedCarId IN :ownedCarIds GROUP BY p.ownedCarId")
    fun countByOwnedCarIds(@Param("ownedCarIds") ownedCarIds: Collection<Long>): List<Array<Any>>

    // 탈퇴 트랜잭션 안에서 실행되므로 영속성 컨텍스트를 비우지 않는다 (비우면 아직 반영 전인 회원 탈퇴 변경이 사라진다)
    @Modifying
    @Query(
        """
        UPDATE Post p SET p.status = ijiri.ijiriserver.community.post.entity.PostStatus.AUTHOR_WITHDRAWN
        WHERE p.memberId = :memberId AND p.status = ijiri.ijiriserver.community.post.entity.PostStatus.PUBLIC
        """
    )
    fun hideAllByAuthor(@Param("memberId") memberId: Long)
}
