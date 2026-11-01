package ijiri.ijiriserver.domain.auth.token.application.port.in;

public interface SignOutTokenUseCase {

    void signOut(Long memberId);
}
