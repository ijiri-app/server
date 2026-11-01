package ijiri.ijiriserver.domain.post.application.port.in;

public interface DeletePostUseCase {

    void delete(Long memberId, Long postId);
}
