package ijiri.ijiriserver.domain.post.application.port.out;

import ijiri.ijiriserver.domain.post.entity.PostPart;

import java.util.List;
import java.util.Optional;

public interface PostPartPort {

    List<PostPart> findAllById(Iterable<Long> ids);

    Optional<PostPart> findById(Long id);
}
