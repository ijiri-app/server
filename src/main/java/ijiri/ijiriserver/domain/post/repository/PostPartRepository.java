package ijiri.ijiriserver.domain.post.repository;

import ijiri.ijiriserver.domain.post.entity.PostPart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostPartRepository extends JpaRepository<PostPart, Long> {
}
