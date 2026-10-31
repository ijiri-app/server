package ijiri.ijiriserver.domain.post.adapter.out.persistence;

import ijiri.ijiriserver.domain.post.application.port.out.PostPort;
import ijiri.ijiriserver.domain.post.entity.Post;
import ijiri.ijiriserver.domain.post.entity.PostStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostPersistenceAdapter implements PostPort {

    private final PostJpaRepository postJpaRepository;

    @Override
    public List<Post> findAllByMemberId(Long memberId) {
        return postJpaRepository.findAllByMemberId(memberId);
    }

    @Override
    public long countByMemberIdAndStatus(Long memberId, PostStatus status) {
        return postJpaRepository.countByMemberIdAndStatus(memberId, status);
    }

    @Override
    public List<Object[]> countByOwnedCarIds(Collection<Long> ownedCarIds) {
        return postJpaRepository.countByOwnedCarIds(ownedCarIds);
    }

    @Override
    public void hideAllByAuthor(Long memberId) {
        postJpaRepository.hideAllByAuthor(memberId);
    }

    @Override
    public void delete(Post entity) {
        postJpaRepository.delete(entity);
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postJpaRepository.findById(id);
    }

    @Override
    public Post save(Post entity) {
        return postJpaRepository.save(entity);
    }

    @Override
    public void flush() {
        postJpaRepository.flush();
    }

    @Override
    public List<Post> findNewest(Specification<Post> spec, int limit) {
        return postJpaRepository.findBy(spec, query -> query
                .sortBy(Sort.by(Sort.Direction.DESC, "id"))
                .limit(limit)
                .all()
        );
    }
}
