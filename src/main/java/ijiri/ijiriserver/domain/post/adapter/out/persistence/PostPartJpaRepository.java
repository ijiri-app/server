package ijiri.ijiriserver.domain.post.adapter.out.persistence;

import ijiri.ijiriserver.domain.post.entity.PostPart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostPartJpaRepository extends JpaRepository<PostPart, Long> {
}
