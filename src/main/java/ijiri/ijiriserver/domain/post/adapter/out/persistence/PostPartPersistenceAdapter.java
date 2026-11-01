package ijiri.ijiriserver.domain.post.adapter.out.persistence;

import ijiri.ijiriserver.domain.post.application.port.out.PostPartPort;
import ijiri.ijiriserver.domain.post.entity.PostPart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostPartPersistenceAdapter implements PostPartPort {

    private final PostPartJpaRepository postPartJpaRepository;

    @Override
    public List<PostPart> findAllById(Iterable<Long> ids) {
        return postPartJpaRepository.findAllById(ids);
    }

    @Override
    public Optional<PostPart> findById(Long id) {
        return postPartJpaRepository.findById(id);
    }
}
