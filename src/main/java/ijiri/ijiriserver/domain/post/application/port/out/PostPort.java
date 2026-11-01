package ijiri.ijiriserver.domain.post.application.port.out;

import ijiri.ijiriserver.domain.post.entity.Post;
import ijiri.ijiriserver.domain.post.entity.PostStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostPort {

    List<Post> findAllByMemberId(Long memberId);

    long countByMemberIdAndStatus(Long memberId, PostStatus status);

    List<Object[]> countByOwnedCarIds(Collection<Long> ownedCarIds);

    void hideAllByAuthor(Long memberId);

    void delete(Post entity);

    Optional<Post> findById(Long id);

    Post save(Post entity);

    void flush();

    List<Post> findNewest(Specification<Post> spec, int limit);
}
