package ijiri.ijiriserver.domain.post.repository;

import ijiri.ijiriserver.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    List<Post> findAllByMemberId(Long memberId);

    // 탈퇴 트랜잭션 안에서 실행되므로 영속성 컨텍스트를 비우지 않는다 (비우면 아직 반영 전인 회원 탈퇴 변경이 사라진다)
    @Modifying
    @Query("""
            UPDATE Post p SET p.status = ijiri.ijiriserver.domain.post.entity.PostStatus.AUTHOR_WITHDRAWN
            WHERE p.memberId = :memberId AND p.status = ijiri.ijiriserver.domain.post.entity.PostStatus.PUBLIC
            """)
    void hideAllByAuthor(@Param("memberId") Long memberId);
}
